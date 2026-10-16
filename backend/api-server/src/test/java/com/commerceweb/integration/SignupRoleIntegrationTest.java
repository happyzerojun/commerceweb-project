package com.commerceweb.integration;

import com.commerceweb.entity.UserRole;
import com.commerceweb.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 HTTP 경로(필터 체인 → 컨트롤러 → 서비스 → PostgreSQL)로 회원가입 역할 상승을 막는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("회원가입 역할 통합 테스트")
class SignupRoleIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    private String signupJson(String email, String role) {
        return """
                {"email":"%s","password":"pw1234","name":"tester","role":"%s"}
                """.formatted(email, role);
    }

    @Test
    @DisplayName("POST /api/auth/signup 에 role=ADMIN을 보내도 CUSTOMER로 저장·응답된다")
    void adminSignupIsDowngraded() throws Exception {
        String email = "signup-" + UUID.randomUUID() + "@test.local";

        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson(email, "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertEquals(UserRole.CUSTOMER, userRepository.findByEmail(email).orElseThrow().getRole());
    }

    @Test
    @DisplayName("role=SELLER 가입은 그대로 SELLER")
    void sellerSignupIsKept() throws Exception {
        String email = "signup-" + UUID.randomUUID() + "@test.local";

        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson(email, "SELLER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SELLER"));

        assertEquals(UserRole.SELLER, userRepository.findByEmail(email).orElseThrow().getRole());
    }
}
