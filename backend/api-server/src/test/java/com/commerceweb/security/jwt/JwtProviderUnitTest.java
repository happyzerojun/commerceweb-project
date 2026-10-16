package com.commerceweb.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring 컨텍스트 없이 JwtProvider만 검증한다.
 * TS-01: 서명이 위조된 토큰에서 예외가 새어 나가 500이 되던 문제의 회귀 테스트.
 */
@DisplayName("JwtProvider 단위 테스트")
class JwtProviderUnitTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef-0123456789";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "jwtExpirationMs", 60_000L);
    }

    @Test
    @DisplayName("정상 토큰은 통과하고 subject를 꺼낼 수 있다")
    void validToken() {
        String token = jwtProvider.generateToken("1");

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.extractUserId(token)).isEqualTo("1");
    }

    @Test
    @DisplayName("다른 키로 서명한 위조 토큰은 예외 없이 false")
    void forgedSignature() {
        String forged = Jwts.builder()
                .setSubject("1")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("another-secret-key-0123456789abcdef-0123456789".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        assertThatCode(() -> jwtProvider.validateToken(forged)).doesNotThrowAnyException();
        assertThat(jwtProvider.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 false")
    void expiredToken() {
        ReflectionTestUtils.setField(jwtProvider, "jwtExpirationMs", -1_000L);
        String expired = jwtProvider.generateToken("1");

        assertThat(jwtProvider.validateToken(expired)).isFalse();
    }

    @Test
    @DisplayName("형식이 깨진 토큰과 빈 토큰은 false")
    void malformedToken() {
        assertThat(jwtProvider.validateToken("invalid.token.here")).isFalse();
        assertThat(jwtProvider.validateToken("")).isFalse();
    }
}
