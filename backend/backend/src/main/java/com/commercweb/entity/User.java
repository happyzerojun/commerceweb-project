package com.commercweb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data   // @를 눌러서 User 클래스의 getter/setter을 자동으로 만들어짐.
@NoArgsConstructor // 기본 생성자 자동으로 만들어줌.
@AllArgsConstructor // 모든 필드를 받는 생성자 만들어 줌.
public class User{  // DB 테이블 구축

    @Id // User테이블에서 PK를 뜻함.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null 값을 허용하지 않고, 중복X, 길이 255제한
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // null 값을 허용하지 않고, 길이 255제한
    @Column(nullable = false, length = 255)
    private String password;    // BCrypt로 암호화될 예정

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)    // Enum 값을 DB에 문자열로 저장.
    @Column(nullable = false)
    private UserRole role;  // ADMIN, SELLER, CUSTOMER 중 하나

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 날짜와 시간을 저장하는 자료형

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 날짜와 시간을 저장하는 자료형 즉, 수정 이후

    // JPA 생명주기 콜백
    @PrePersist // Insert문 이후 즉, 데이터가 들어온 이후
    protected void onCreate() {
        createdAt = LocalDateTime.now();    // 현재 시각을 기준으로 변경
        updatedAt = LocalDateTime.now();    // 현재 시각을 기준으로 변경
    }

    @PreUpdate // Update문 이후, 즉, 데이터 변경 이후
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(); // 업데이트 이후 현재 시각을 기준으로 변경
    }


}