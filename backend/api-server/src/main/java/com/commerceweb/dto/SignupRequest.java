package com.commerceweb.dto;

import com.commerceweb.entity.UserRole; // ✅ UserRole 임포트 추가

public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private UserRole role; // 희망 역할(구매자/판매자). 서버가 화이트리스트로 걸러 저장하므로 ADMIN은 무시된다

    // 기본 생성자
    public SignupRequest() {}

    // 전체 생성자 (role 추가)
    public SignupRequest(String email, String password, String name, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    // Getters & Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // ✅ 추가된 필드의 Getter & Setter
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}