package com.commerceweb.dto;

/**
 * 인증 응답 DTO (Data Transfer Object)
 *
 * 역할: 사용자가 로그인 또는 회원가입에 성공했을 때,
 *      서버에서 프론트엔드(클라이언트)로 돌려주는 정보를 담는 클래스
 *
 * 프론트엔드가 받을 정보:
 * 1. token        → 이후 API 요청할 때 인증 헤더에 담을 JWT 토큰
 * 2. userId       → 사용자 고유 ID (DB에 저장된 PK)
 * 3. email        → 로그인한 사용자 이메일
 * 4. name         → 로그인한 사용자 이름
 * 5. role         → 사용자 권한 (ADMIN, USER 등) → 프론트엔드에서 UI 권한 결정
 */
public class AuthResponse {

    // ========== 필드(속성) ==========

    /** JWT 토큰 - 이후 모든 인증이 필요한 API 요청에 사용됨 */
    private String token;

    /** 사용자 DB 식별자 (Primary Key) - 사용자 정보 조회/수정할 때 필요 */
    private Long userId;

    /** 사용자 이메일 - 로그인 시 사용했던 이메일 */
    private String email;

    /** 사용자 이름 - UI에 "OOO님 반갑습니다" 같이 표시할 때 사용 */
    private String name;

    /**
     * ✅ 추가된 필드: 사용자 권한
     * 역할: 프론트엔드에서 사용자 권한에 따라 버튼/메뉴 표시 여부 결정
     * 예시:
     *   - "ADMIN" → 관리자 페이지 접근 가능
     *   - "USER"  → 일반 사용자 기능만 표시
     *   - "GUEST" → 제한된 기능만 표시
     * 이렇게 하면 서버에서 검사 외에 프론트엔드에서도 먼저 UI 제어 가능
     */
    private String role;


    // ========== 생성자 ==========

    /**
     * 5개 인자를 받는 생성자 (✅ 수정됨: role 추가)
     *
     * @param token  JWT 토큰
     * @param userId 사용자 ID
     * @param email  사용자 이메일
     * @param name   사용자 이름
     * @param role   ✅ 추가된 파라미터: 사용자 권한
     *
     * 사용 예시:
     * AuthResponse response = new AuthResponse(
     *     "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // token
     *     1L,                                           // userId
     *     "user@example.com",                           // email
     *     "김철수",                                      // name
     *     "ADMIN"                                       // role ✅ 새로 추가
     * );
     */
    public AuthResponse(String token, Long userId, String email, String name, String role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;  // ✅ role 필드에 값 저장
    }


    // ========== Getter 메소드 (값을 읽기만 함) ==========

    /**
     * JWT 토큰 반환
     * @return JWT 토큰 문자열
     */
    public String getToken() {
        return token;
    }

    /**
     * 사용자 ID 반환
     * @return 사용자 고유 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 사용자 이메일 반환
     * @return 사용자 이메일 주소
     */
    public String getEmail() {
        return email;
    }

    /**
     * 사용자 이름 반환
     * @return 사용자 이름
     */
    public String getName() {
        return name;
    }


    /**
     * ✅ 추가된 Getter: 사용자 권한 반환
     *
     * 프론트엔드에서 이 메소드를 호출해서 권한을 체크함
     *
     * 사용 예시 (JavaScript):
     * const response = await fetch('/api/login', ...);
     * const data = await response.json();
     *
     * if (data.role === 'ADMIN') {
     *     // 관리자 메뉴 표시
     *     showAdminMenu();
     * } else if (data.role === 'USER') {
     *     // 일반 사용자 메뉴 표시
     *     showUserMenu();
     * }
     *
     * @return 사용자 권한 ("ADMIN", "USER", "GUEST" 등)
     */
    public String getRole() {
        return role;
    }
}
