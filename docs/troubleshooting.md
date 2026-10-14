# 트러블슈팅 기록

개발·셀프 코드 리뷰 중 발견한 문제와 해결 과정을 남겨요. 형식은 **증상 → 원인 → 해결 → 재발 방지**예요.
번호는 `TS-NN`으로 이어 붙이고 지우지 않아요.

| ID | 날짜 | 분류 | 제목 | 상태 |
|---|---|---|---|---|
| TS-01 | 2026-10-14 | 보안 | 서명이 위조된 JWT가 401이 아니라 500이 될 수 있음 | 해결 |
| TS-02 | 2026-10-14 | 추천 | 점수순으로 고른 추천 상품의 순위가 DB 반환 순서로 뒤섞임 | 해결 |
| TS-03 | 2026-01-09 / 2026-10-14 | 배포 | Nginx `proxy_pass` 끝 슬래시 때문에 `/api` 경로가 잘림 | 해결 |
| TS-04 | 2026-10-14 | 배포 | 프론트가 `http://localhost:8080`을 직접 호출함 | 해결 |
| TS-05 | 2026-10-14 | 통계 | 판매 통계를 상품 이름으로 묶어 같은 이름 상품의 매출이 합쳐짐 | 해결 |
| TS-06 | 2026-10-14 | 리뷰 | 별점 범위 검증 없음, 삭제 후 평점 미갱신, 리뷰 쓰기 API가 필터에서 공개 | 해결 |
| TS-07 | 2026-10-14 | 주문 | 리뷰가 달린 주문을 취소하면 FK 제약 위반으로 500 | 임시 해결 (소프트 취소는 다음 단계) |
| 이전 | 2026-01 | — | 404 경로 불일치, 로그인 응답 role 누락, 판매자 주문 403, Redis 직렬화 실패 | README 트러블슈팅 표 참고 |

---

### TS-01 서명이 위조된 JWT가 401이 아니라 500이 될 수 있음
- **증상:** 다른 키로 서명한 토큰을 `Authorization: Bearer ...`로 보내면 인증 실패(401)가 아니라 서버 오류가 날 수 있는 구조. 기존 테스트(`"invalid.token.here"`)는 형식 오류만 확인해서 통과하고 있었음.
- **원인:** `JwtProvider.validateToken()`이 `catch (SecurityException | MalformedJwtException e)`로 잡고 있었는데, `import io.jsonwebtoken.*`에는 `SecurityException`이 없어 **`java.lang.SecurityException`**으로 해석됨. jjwt 0.11의 서명 불일치 예외(`SignatureException`)는 `io.jsonwebtoken.security.SecurityException` 계열이라 어느 catch에도 걸리지 않고, 필터에서 `validateToken()` 호출은 try 블록 밖에 있어 예외가 그대로 전파됨.
- **해결:** `ExpiredJwtException`은 DEBUG로, 나머지는 상위 타입 `JwtException | IllegalArgumentException`으로 한 번에 잡아 `false`를 돌려줌.
- **재발 방지:** `JwtProviderUnitTest`에 **다른 키로 서명한 토큰**, 만료 토큰, 빈 토큰 케이스를 추가. 예외는 와일드카드 import에 기대지 말고 라이브러리의 상위 예외 타입으로 잡아요.

### TS-02 점수순으로 고른 추천 상품의 순위가 DB 반환 순서로 뒤섞임
- **증상:** 예상 점수 1등 상품이 추천 목록 맨 위에 오지 않을 수 있음.
- **원인:**
  1. 점수순으로 정렬한 ID 목록으로 `findByIdIn(ids)`를 호출해 그 결과를 그대로 반환. `IN` 조회는 결과 순서를 보장하지 않음.
  2. 점수 병합이 `(old + new) / 2`라 입력 순서에 따라 값이 달라짐 (평점 5,5,1 → 3.0 / 1,5,5 → 4.5, 실제 평균 3.67).
  3. 기존 Mockito 테스트는 가짜 저장소가 **내가 정한 순서대로** 결과를 돌려줘서 1번을 잡지 못함.
- **해결:** ID → 상품 맵을 만든 뒤 점수순 ID 목록 기준으로 순서를 복원. 점수는 공통 평가 수를 가중치로 쓰는 **가중 평균**으로 바꾸고, 동점이면 상품 ID로 정렬해 결과를 고정. 유사 사용자 쿼리에 `LIMIT 5`를 넣고, 이미 평가한 상품 목록은 `HashSet`으로 검사.
- **재발 방지:** 저장소가 **다른 순서로** 돌려주는 경우와 평균이 입력 순서와 무관한지를 검증하는 테스트 2건 추가 (`RecommendationServiceTest`).

### TS-03 Nginx `proxy_pass` 끝 슬래시 때문에 `/api` 경로가 잘림
- **증상:** Nginx를 거치면 `/api/products`가 백엔드에 `/products`로 전달되어 404.
- **원인:** `location /api/ { proxy_pass http://commerceweb-backend:8080/; }`처럼 `proxy_pass`에 URI(끝 `/`)가 있으면 Nginx는 location에 매칭된 `/api/`를 그 URI로 치환함. 컨트롤러는 `/api/...`로 매핑돼 있음.
- **해결:** `proxy_pass http://commerceweb-backend:8080;`(URI 없음)으로 경로를 그대로 전달하고 `Host`, `X-Forwarded-*` 헤더를 넘김.
- **재발 방지:** 프론트·Nginx·컨트롤러가 같은 `/api` 접두사를 쓰는지 배포 전 확인. 아래 TS-04와 함께 봐야 하는 문제예요(TS-04 때문에 이 버그가 가려져 있었음).

### TS-04 프론트가 `http://localhost:8080`을 직접 호출함
- **증상:** 내 PC에서는 동작하지만, 배포된 화면을 다른 사람이 열면 **그 사람 PC의** `localhost:8080`을 호출하게 됨.
- **원인:** `api.js`의 기본 `baseURL`이 `http://localhost:8080`이고, `authService.js`는 별도 axios와 하드코딩 주소를 사용. Docker 빌드에서 `VITE_API_URL`을 주입하지 않음.
- **해결:** 기본 `baseURL`을 같은 출처(`''`)로 바꾸고 `authService`도 공통 `api` 인스턴스를 사용. 개발 환경은 `vite.config.js`의 `/api` 프록시로 같은 상대 경로를 씀.
- **재발 방지:** 배포 후 **다른 기기**에서 접속해 확인. 같은 출처로 서빙하면 운영 환경에서는 CORS 설정도 필요 없어요.

### TS-05 판매 통계를 상품 이름으로 묶어 같은 이름 상품의 매출이 합쳐짐
- **증상:** 이름이 같은 서로 다른 상품의 매출·수량이 한 줄로 집계될 수 있음.
- **원인:** JPQL `GROUP BY p.name, p.averageRating`.
- **해결:** `GROUP BY p.id, p.name, p.averageRating ORDER BY SUM(o.totalPrice) DESC`.
- **남은 일:** 상품-판매자 관계가 없어 판매자별 통계가 아니라 전체 통계가 보여요. `product.seller_id` 추가 후 본인 상품만 집계해야 해요.

### TS-06 별점 범위 검증 없음, 삭제 후 평점 미갱신, 리뷰 쓰기 API가 필터에서 공개
- **증상:** 별점 100도 저장 가능 → 평균 평점과 인기 순위 왜곡. 리뷰를 지워도 상품 평점·리뷰 수가 그대로. `/api/reviews/**` 전체가 `permitAll()`이라 쓰기 요청도 필터를 통과(컨트롤러 내부 검사에만 의존).
- **해결:** 작성·수정 시 1~5 검증, 삭제 후 `updateProductRating()` 호출, 리뷰는 `GET`만 공개. `review.order_id`에 유니크 제약 추가(동시 중복 작성 차단). `IllegalArgumentException`/`IllegalStateException`/`AccessDeniedException`을 400/409/403으로 바꾸는 `GlobalExceptionHandler` 추가(이전에는 전부 500). 존재하지 않는 역할 `"USER"`를 권한 규칙에서 제거.
- **남은 일:** 동시 작성 시 평균 평점이 나중 커밋으로 덮일 수 있어요. DB 집계 UPDATE 한 번으로 갱신하는 방식으로 바꿀 예정이에요.

### TS-07 리뷰가 달린 주문을 취소하면 FK 제약 위반으로 500
- **증상(코드 분석):** 주문 취소가 `orderRepository.delete()`라서, 리뷰가 참조 중인 주문을 지우면 `reviews.order_id`(NOT NULL FK) 제약 위반.
- **임시 해결:** 리뷰가 있는 주문은 409 "리뷰가 작성된 주문은 취소할 수 없습니다."로 막음.
- **다음 단계:** 주문 상태를 enum(`PENDING/PAID/FAILED/CANCELED`)으로 바꾸고 취소를 상태 변경(소프트 취소)으로 전환. 매출 통계는 `PAID`만 집계.

---

## CI
`.github/workflows/ci.yml` — PR마다 PostgreSQL·Redis 서비스 컨테이너를 띄워 `./mvnw test`를 실행하고, 프론트는 `npm run build`를 확인해요.
