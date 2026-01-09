package com.commerceweb.security.jwt;

import com.commerceweb.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BackendApplication.class)
public class JwtProviderTest {

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    public void testGenerateToken() {
        // 1️⃣ 토큰 생성 (수정: Role 인자 추가)
        String token = jwtProvider.generateToken("user@example.com");

        // 2️⃣ 토큰이 null이 아닌지 확인
        assertNotNull(token, "Token should not be null");
        System.out.println("✅ Generated Token: " + token);

        // 3️⃣ 토큰이 "." 를 포함하는지 확인 (header.payload.signature)
        assertTrue(token.contains("."), "Token should contain dots");
        System.out.println("✅ Token format is correct (contains dots)");
    }

    @Test
    public void testValidateToken() {
        // 1️⃣ 토큰 생성 (수정: Role 인자 추가)
        String token = jwtProvider.generateToken("user@example.com");

        // 2️⃣ 토큰 검증
        boolean isValid = jwtProvider.validateToken(token);

        // 3️⃣ 유효해야 함
        assertTrue(isValid, "Token should be valid");
        System.out.println("✅ Token validation passed");
    }

    @Test
    public void testExtractUserId() {
        // 1️⃣ 토큰 생성 (수정: Role 인자 추가)
        String userId = "user@example.com";
        String token = jwtProvider.generateToken(userId);

        // 2️⃣ userId 추출
        String extractedUserId = jwtProvider.extractUserId(token);

        // 3️⃣ 추출된 userId가 원래와 같은지 확인
        assertEquals(userId, extractedUserId, "Extracted userId should match");
        System.out.println("✅ Extracted UserId: " + extractedUserId);
    }

    @Test
    public void testInvalidToken() {
        // 1️⃣ 잘못된 토큰
        String invalidToken = "invalid.token.here";

        // 2️⃣ 검증 (실패해야 함)
        boolean isValid = jwtProvider.validateToken(invalidToken);

        // 3️⃣ 유효하지 않아야 함
        assertFalse(isValid, "Invalid token should not be valid");
        System.out.println("✅ Invalid token correctly rejected");
    }
}