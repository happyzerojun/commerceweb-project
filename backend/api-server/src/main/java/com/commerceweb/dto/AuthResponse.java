package com.commerceweb.dto;

public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String name;
    private String role;       // ✅ 추가: 프론트엔드 권한 확인용

    // 생성자 (인자 5개로 수정)
    public AuthResponse(String token, Long userId, String email, String name, String role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role; // ✅ 추가
    }

    // Getters
    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }

    // ✅ 추가된 필드의 Getter
    public String getRole() { return role; }
}