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
| TS-08 | 2026-10-16 | 추천 | 리뷰가 추천 데이터(ratings)에 반영되지 않고, 추천 캐시가 실제 흐름에서 무효화되지 않음 | 해결 |
| TS-09 | 2026-10-16 | 추천 | 유사 사용자 네이티브 쿼리에 쿼리에서 쓰지 않는 파라미터 | 해결 |
| TS-10 | 2026-10-16 | 리뷰 | 동시 리뷰 작성 시 상품 평균 평점 Lost Update | 해결 |
| TS-11 | 2026-10-16 | 상품 | 동시 조회 시 조회수 Lost Update | 해결 |
| TS-12 | 2026-10-16 | 보안 | 회원가입 요청의 role로 ADMIN 계정을 만들 수 있음 (권한 상승) | 해결 |
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
- **해결:** 작성·수정 시 1~5 검증, 삭제 후 평점 재계산, 리뷰는 `GET`만 공개. `review.order_id`에 유니크 제약 `uk_review_order` 추가(동시 중복 작성 차단). `IllegalArgumentException`/`IllegalStateException`/`AccessDeniedException`을 400/409/403으로 바꾸는 `GlobalExceptionHandler` 추가(이전에는 전부 500). 존재하지 않는 역할 `"USER"`를 권한 규칙에서 제거.
- **추가 수정 (2026-10-16):** 중복 사전 검사(`existsByOrderId`)가 `RuntimeException`이라 여전히 500이었음 → `IllegalStateException`(409). 사전 검사를 동시에 통과한 두 번째 INSERT가 유니크 제약에 걸리는 경우도 `uk_review_order` 위반만 골라 409로 변환하고, 다른 제약 위반은 그대로 올림. 남은 `DataIntegrityViolationException`은 핸들러에서 409로 받는 안전망. 같은 주문에 10건 동시 작성 → 1건만 저장되는 통합 테스트.
- **남은 일:** ~~동시 작성 시 평균 평점이 나중 커밋으로 덮일 수 있음~~ → TS-10에서 해결.

### TS-07 리뷰가 달린 주문을 취소하면 FK 제약 위반으로 500
- **증상(코드 분석):** 주문 취소가 `orderRepository.delete()`라서, 리뷰가 참조 중인 주문을 지우면 `reviews.order_id`(NOT NULL FK) 제약 위반.
- **임시 해결:** 리뷰가 있는 주문은 409 "리뷰가 작성된 주문은 취소할 수 없습니다."로 막음.
- **다음 단계:** 주문 상태를 enum(`PENDING/PAID/FAILED/CANCELED`)으로 바꾸고 취소를 상태 변경(소프트 취소)으로 전환. 매출 통계는 `PAID`만 집계.

### TS-08 리뷰가 추천 데이터(ratings)에 반영되지 않고, 추천 캐시가 실제 흐름에서 무효화되지 않음
- **증상:** 리뷰를 아무리 써도 개인화 추천이 늘 인기 상품(Cold Start)으로만 나옴. 추천 결과가 캐시에 한번 들어가면 지워지지 않음.
- **원인:**
  1. 협업 필터링은 `ratings` 테이블을 읽는데, `ratings`에 쓰는 `RatingService.rateProduct()`를 호출하는 컨트롤러가 없었음. 실제 평점은 리뷰(`review`)로만 들어옴.
  2. 캐시 무효화(`@CacheEvict`)도 그 호출되지 않는 `RatingService`에만 붙어 있었음. Redis 캐시에 TTL도 없어 한번 들어간 추천이 계속 남음.
  3. `rateProduct()`는 상품 평균을 `ratings` 기준으로 다시 덮어써서, 연결했다면 리뷰 기준 평균과 충돌했을 구조.
- **해결:**
  - 원천을 리뷰 하나로 정하고 `ratings`는 추천용 투영으로 유지: 리뷰 작성·수정·삭제와 **같은 트랜잭션**에서 (사용자, 상품)의 최신 리뷰 별점을 `INSERT ... ON CONFLICT (user_id, product_id) DO UPDATE`로 upsert, 남은 리뷰가 없으면 삭제. 조회 후 INSERT가 아니라 한 문장이라 경쟁 조건이 없음. 미사용 `rateProduct()`는 제거.
  - 리뷰 쓰기 메서드에 `@CacheEvict(value = "recommendations", allEntries = true)`. **allEntries인 이유:** 작성자뿐 아니라 작성자와 취향이 비슷한 다른 사용자의 추천도 바뀌고, 키가 `userId_limit`이라 한 사용자 키만 정확히 지울 수도 없음.
  - `RedisCacheManager`를 `transactionAware`로: 무효화를 **커밋 후에** 실행(커밋 전에 지우면 그 사이 요청이 옛 데이터로 다시 캐시를 채울 수 있고, 롤백돼도 캐시만 지워짐). allEntries 삭제는 `KEYS` 대신 `SCAN`(1000개씩)으로 Redis 블로킹 방지. TTL 1시간(`spring.cache.redis.time-to-live=1h`)을 안전망으로.
- **트레이드오프:** 리뷰가 잦으면 전체 추천 캐시가 자주 비워져 적중률이 떨어짐. 그때는 사용자별 버전 키(`userId:v{n}`)나 TTL만 쓰는 방식으로 바꿀 수 있음. 커밋 직전에 시작된 추천 계산이 커밋 후 옛 결과를 넣는 좁은 경쟁은 TTL이 상한.
- **운영 반영(1회):** 배포 전부터 있던 리뷰는 `ratings`에 없으므로 한 번 채우고, 상품 평균도 리뷰 기준으로 다시 맞춰요. 이후에는 리뷰 쓰기마다 자동 동기화돼요.
  ```sql
  -- 1) (사용자, 상품)별 최신 리뷰 별점으로 ratings 채우기
  INSERT INTO ratings (user_id, product_id, score, review, created_at, updated_at)
  SELECT DISTINCT ON (user_id, product_id) user_id, product_id, rating, content, now(), now()
  FROM review WHERE rating IS NOT NULL
  ORDER BY user_id, product_id, created_at DESC, id DESC
  ON CONFLICT (user_id, product_id) DO UPDATE SET score = EXCLUDED.score, review = EXCLUDED.review, updated_at = now();
  -- 2) 리뷰 근거가 없는 ratings 정리 (필요 시, 실행 전 SELECT로 건수 확인)
  DELETE FROM ratings t WHERE NOT EXISTS (
    SELECT 1 FROM review r WHERE r.user_id = t.user_id AND r.product_id = t.product_id AND r.rating IS NOT NULL);
  -- 3) 상품 평균·개수를 리뷰 기준으로 재계산
  UPDATE products p SET
    average_rating = COALESCE((SELECT ROUND(AVG(r.rating), 1) FROM review r WHERE r.product_id = p.id), 0),
    rating_count   = (SELECT COUNT(r.rating) FROM review r WHERE r.product_id = p.id);
  ```
  배포 직후 Redis의 `recommendations::*` 키도 한 번 비우면(또는 TTL 1시간 대기) 바로 반영돼요.
- **재발 방지:** `RecommendationCacheIntegrationTest` — 리뷰만으로 추천이 나오는지(인기 상품 대체 경로로는 나올 수 없는 평균 3.0 상품이 추천됨), 쓰기 커밋 후 캐시 제거, 실패한 쓰기는 캐시 유지, TTL, 최신 별점 반영·삭제.

### TS-09 유사 사용자 네이티브 쿼리에 쿼리에서 쓰지 않는 파라미터
- **증상(코드 분석):** `findSimilarUsers(Long userId, List<Long> ratedProductIds)`인데 쿼리에는 `:userId`만 있음.
- **원인:** Spring Boot 부모 POM은 `-parameters`로 컴파일하므로 `ratedProductIds`도 이름 있는 파라미터로 인식됨. Spring Data JPA는 쿼리에 없는 이름 있는 파라미터를 기동 시 검증(`parameter not found in annotated query`)할 수 있어 버전에 따라 기동 실패 위험(확인 필요). 서비스는 단계별 예외를 잡아 인기 상품으로 대체하므로, 실행 시 실패하더라도 기존 테스트로는 드러나지 않는 구조였음.
- **해결:** 셀프 조인이 이미 "내가 평가한 상품"을 거르므로 파라미터 제거, 이름은 `@Param`으로 명시.
- **재발 방지:** 서비스를 거치지 않고 저장소 메서드를 PostgreSQL에서 직접 실행하는 `RatingRepositoryIntegrationTest`.

### TS-10 동시 리뷰 작성 시 상품 평균 평점 Lost Update
- **증상:** 한 상품에 리뷰가 동시에 달리면 평균·리뷰 수가 일부 리뷰를 빼고 계산된 값으로 남을 수 있음.
- **원인:** 리뷰 목록을 읽어 자바에서 평균을 계산하고 `product.save()`로 덮어씀. 두 트랜잭션이 서로의 리뷰를 보지 못한 채 계산하면 나중 커밋이 앞선 결과를 덮음.
- **해결:**
  1. `UPDATE products SET average_rating = COALESCE((SELECT ROUND(AVG(rating), 1) FROM review ...), 0), rating_count = (SELECT COUNT(rating) ...) WHERE id = ?` 한 문장으로 DB가 직접 집계.
  2. **그 전에 `SELECT id FROM products WHERE id = ? FOR NO KEY UPDATE`로 상품 행을 잠금.** PostgreSQL READ COMMITTED에서는 위 UPDATE도 단독으로는 안전하지 않음: 행 잠금을 기다린 뒤 **대상 행만** 최신 버전으로 다시 읽고, 서브쿼리(`review` 집계)는 문장 시작 시점 스냅샷을 그대로 써서 먼저 커밋된 리뷰가 빠질 수 있음. 잠금을 먼저 잡으면 다음 문장이 새 스냅샷으로 시작함.
  3. `FOR UPDATE`가 아니라 `FOR NO KEY UPDATE`: 리뷰·주문 INSERT의 FK 검사가 상품 행에 거는 `FOR KEY SHARE`와 충돌하지 않아 "서로 KEY SHARE를 쥔 채 FOR UPDATE를 기다리는" 교착 상태가 없음.
  4. 작성·수정·삭제 모두 리뷰 반영(flush) → 상품 잠금 → ratings 동기화 → 집계 순서로 통일해 잠금 순서를 고정.
- **트레이드오프:** 같은 상품의 리뷰 쓰기는 직렬화됨(다른 상품끼리는 영향 없음). 리뷰 수가 아주 많아지면 매번 AVG 집계 비용이 커지므로, 그때는 `rating_sum`/`rating_count` 컬럼을 `+=`로 증감하는 방식(원자적 증감은 잠금 재평가 시 다시 계산됨)으로 바꿀 수 있음.
- **재발 방지:** 리뷰 20건 동시 작성 → 개수 20, 평균 3.0 / 수정·삭제 후 재집계 통합 테스트(`ReviewConcurrencyIntegrationTest`).

### TS-11 동시 조회 시 조회수 Lost Update
- **증상:** 상세 조회가 몰리면 조회수가 실제 조회 수보다 적게 늘어남.
- **원인:** `findById` → `setViewCount(viewCount + 1)` → `save()`. 같은 값을 읽은 요청들이 같은 값으로 덮어씀. 또 `@Builder`로 만든 상품은 필드 초기값(`= 0L`)이 무시돼 `view_count`가 `NULL`일 수 있어 `+ 1`에서 NPE 가능.
- **해결:** `UPDATE Product p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :id` 원자적 증가. 행 잠금 재평가 시 `SET` 식이 최신 값으로 다시 계산되므로 정확함. 갱신 행 0건이면 상품 없음.
- **트레이드오프:** 인기 상품은 조회마다 같은 행에 쓰기 잠금이 몰림(핫 로우). 트래픽이 커지면 Redis `INCR`로 모았다가 주기적으로 DB에 반영하는 방식이 다음 단계.
- **재발 방지:** 50명 동시 조회 → 조회수 50, `NULL` 조회수 → 1 통합 테스트(`ProductViewCountConcurrencyTest`).

### TS-12 회원가입 요청의 role로 ADMIN 계정을 만들 수 있음 (권한 상승)
- **증상(코드 분석):** `POST /api/auth/signup`에 `{"role":"ADMIN"}`을 보내면 관리자 계정이 만들어짐. 화면은 구매자/판매자만 고르게 하지만 API는 그대로 받음.
- **원인:** `AuthService.signup()`이 요청 본문의 `role`을 검증 없이 `user.setRole(role)`로 저장. 클라이언트 입력을 권한 결정에 그대로 사용.
- **해결:** 스스로 고를 수 있는 역할을 화이트리스트 `{CUSTOMER, SELLER}`로 제한. 그 외(ADMIN)는 무시하고 CUSTOMER로 저장하며 경고 로그를 남김. 판매자 가입은 판매 통계 기능 때문에 유지. 관리자는 운영자가 DB에서 직접 지정.
- **트레이드오프:** 400으로 거절하는 대신 조용히 CUSTOMER로 저장해 정상 클라이언트 흐름은 깨지지 않지만, 공격 시도는 로그로만 드러남. SELLER도 셀프 가입이라 판매자 승인 절차가 필요해지면 `PENDING_SELLER` 같은 상태를 두는 게 다음 단계.
- **재발 방지:** `AuthServiceTest`(ADMIN 요청 → CUSTOMER 저장, 화이트리스트에 ADMIN 없음), `SignupRoleIntegrationTest`(필터 체인 → 컨트롤러 → DB 경로로 ADMIN 요청 시 응답·저장 role이 CUSTOMER).

---

## CI
`.github/workflows/ci.yml` — PR마다 PostgreSQL·Redis 서비스 컨테이너를 띄워 `./mvnw test`를 실행하고, 프론트는 `npm run build`를 확인해요.
테스트 실행 수는 notice, 실패 시 `[ERROR]` 줄·실패 테스트·`Caused by`는 error 어노테이션으로 PR 화면에 요약돼요.
동시성 테스트는 실제로 커밋해야 재현되므로 `@Transactional` 롤백 대신 테스트가 만든 행만 지워요(`TestFixtures`).
