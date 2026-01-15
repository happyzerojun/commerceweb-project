package com.commerceweb.controller;

import com.commerceweb.security.jwt.JwtProvider;
import com.commerceweb.dto.AuthResponse;
import com.commerceweb.dto.LoginRequest;
import com.commerceweb.dto.SignupRequest;
import com.commerceweb.dto.ErrorResponse;
import com.commerceweb.entity.User;
import com.commerceweb.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 인증 관련 API 컨트롤러
 * - 회원가입, 로그인 기능 제공
 * - JWT 토큰 생성 및 사용자 권한 정보 반환
 */
@RestController                                                    // REST API 컨트롤러로 등록
@RequestMapping("/api/auth")                                      // 모든 엔드포인트가 /api/auth로 시작
@CrossOrigin(origins = "http://localhost:3000")                  // React 개발 서버(3000 포트)의 CORS 요청 허용
public class AuthController {

    // ============ 의존성 필드 (Constructor Injection 사용) ============

    private final AuthService authService;                         // 회원가입/로그인 비즈니스 로직 처리
    private final JwtProvider jwtProvider;                         // JWT 토큰 생성/검증 담당

    // final 키워드: 객체 생성 후 변경 불가능 → 스레드 안전성 보장

    /**
     * 생성자를 통한 의존성 주입 (Constructor Injection)
     * 장점:
     * 1. 필수 의존성을 명시적으로 표현
     * 2. 객체 불변성 보장 (final 사용 가능)
     * 3. 테스트 시 mock 객체 주입이 쉬움
     * 4. NullPointerException 방지
     *
     * @param authService 인증 서비스 객체
     * @param jwtProvider JWT 토큰 제공자 객체
     */
    public AuthController(AuthService authService, JwtProvider jwtProvider) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

    // ============ 회원가입 API ============

    /**
     * POST /api/auth/signup
     * 새로운 사용자를 등록하고 JWT 토큰 발급
     *
     * @param request 회원가입 요청 데이터 (email, password, name, role)
     * @return JWT 토큰과 사용자 정보가 포함된 AuthResponse
     */
    @PostMapping("/signup")                                        // POST 요청을 /api/auth/signup에 매핑
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {  // 요청 본문의 JSON을 SignupRequest로 자동 변환
        try {
            // ===== 1️⃣ 회원가입 처리 =====
            User user = authService.signup(
                    request.getEmail(),                            // 입력된 이메일
                    request.getPassword(),                         // 입력된 비밀번호
                    request.getName(),                             // 입력된 이름
                    request.getRole()                              // SELLER 또는 CUSTOMER 역할
            );
            // authService.signup()이 다음을 처리함:
            // - 이메일 중복 체크
            // - 비밀번호 암호화 (BCryptPasswordEncoder 사용)
            // - User 엔티티를 DB에 저장
            // - 저장된 User 객체 반환

            System.out.println("✅ 회원가입 완료: " + user.getEmail() + " (" + user.getRole() + ")");

            // ===== 2️⃣ JWT 토큰 생성 =====
            String token = jwtProvider.generateToken(String.valueOf(user.getId()));
            // jwtProvider.generateToken()이 다음을 처리함:
            // - 사용자 ID를 기반으로 JWT 토큰 생성
            // - 만료 시간(예: 24시간) 설정
            // - 서명(signature)으로 토큰 무결성 보장

            System.out.println("✅ JWT 토큰 생성: " + token.substring(0, 20) + "...");

            // ===== 3️⃣ 응답 DTO 생성 및 반환 =====
            AuthResponse response = new AuthResponse(
                    token,                                         // 발급된 JWT 토큰 (프론트에서 로컬스토리지에 저장)
                    user.getId(),                                  // 사용자 ID
                    user.getEmail(),                               // 사용자 이메일
                    user.getName(),                                // 사용자 이름
                    user.getRole().name()                          // 사용자 권한 (SELLER/CUSTOMER)
                    // → 프론트에서 메뉴 표시/숨김 결정
            );

            return ResponseEntity.status(HttpStatus.CREATED)       // HTTP 201 Created 상태 코드
                    .body(response);                               // 응답 본문으로 AuthResponse 반환
            // → 프론트에서 JSON으로 자동 변환

        } catch (RuntimeException e) {
            // 회원가입 실패 시 (이메일 중복, 비밀번호 형식 오류 등)
            System.out.println("❌ 회원가입 실패: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)   // HTTP 400 Bad Request
                    .body(new ErrorResponse(e.getMessage()));      // 에러 메시지 반환
        }
    }

    // ============ 로그인 API ============

    /**
     * POST /api/auth/login
     * 이메일과 비밀번호로 사용자를 인증하고 JWT 토큰 발급
     *
     * @param request 로그인 요청 데이터 (email, password)
     * @return JWT 토큰과 사용자 정보가 포함된 AuthResponse
     */
    @PostMapping("/login")                                         // POST 요청을 /api/auth/login에 매핑
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {   // 요청 본문의 JSON을 LoginRequest로 변환
        try {
            // ===== 1️⃣ 로그인 처리 =====
            User user = authService.login(
                    request.getEmail(),                            // 입력된 이메일
                    request.getPassword()                          // 입력된 비밀번호
            );
            // authService.login()이 다음을 처리함:
            // - 이메일로 사용자 조회
            // - 입력된 비밀번호와 DB의 암호화된 비밀번호 비교 (Bcrypt)
            // - 일치하면 User 객체 반환
            // - 불일치하면 RuntimeException 발생

            System.out.println("✅ 로그인 완료: " + user.getEmail() + " (" + user.getRole() + ")");

            // ===== 2️⃣ JWT 토큰 생성 =====
            String token = jwtProvider.generateToken(String.valueOf(user.getId()));
            // 로그인 성공 시 새로운 JWT 토큰 생성

            System.out.println("✅ JWT 토큰 생성: " + token.substring(0, 20) + "...");

            // ===== 3️⃣ 응답 DTO 생성 및 반환 =====
            AuthResponse response = new AuthResponse(
                    token,                                         // 발급된 JWT 토큰
                    user.getId(),                                  // 사용자 ID
                    user.getEmail(),                               // 사용자 이메일
                    user.getName(),                                // 사용자 이름
                    user.getRole().name()                          // 사용자 권한
                    // ✅ SELLER: 판매자 대시보드/통계 페이지 접근 가능
                    // ✅ CUSTOMER: 구매자 페이지만 접근 가능
            );

            return ResponseEntity.ok(response);                    // HTTP 200 OK 상태 코드로 응답

        } catch (RuntimeException e) {
            // 로그인 실패 시 (이메일 없음, 비밀번호 오류 등)
            System.out.println("❌ 로그인 실패: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)  // HTTP 401 Unauthorized
                    .body(new ErrorResponse(e.getMessage()));      // 에러 메시지 반환
        }
    }
}

// ============ 사용 예시 ============
/*
 * 1. 회원가입 요청 (프론트에서)
 *    POST http://localhost:8080/api/auth/signup
 *    {
 *      "email": "user@example.com",
 *      "password": "password123",
 *      "name": "김철수",
 *      "role": "CUSTOMER"  또는 "SELLER"
 *    }
 *
 * 2. 회원가입 응답 (백엔드에서)
 *    HTTP 201 Created
 *    {
 *      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *      "userId": 1,
 *      "email": "user@example.com",
 *      "name": "김철수",
 *      "role": "CUSTOMER"
 *    }
 *
 * 3. 로그인 요청
 *    POST http://localhost:8080/api/auth/login
 *    {
 *      "email": "user@example.com",
 *      "password": "password123"
 *    }
 *
 * 4. 로그인 응답 (토큰과 함께)
 *    HTTP 200 OK
 *    {
 *      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *      "userId": 1,
 *      "email": "user@example.com",
 *      "name": "김철수",
 *      "role": "CUSTOMER"
 *    }
 *
 * 5. 이후 API 호출 시 (인증이 필요한 경우)
 *    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *    → JwtAuthenticationFilter가 토큰 검증
 *    → 토큰에서 사용자 ID 추출
 *    → SecurityContext에 사용자 정보 저장
 */