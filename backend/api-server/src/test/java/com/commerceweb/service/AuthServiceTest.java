package com.commerceweb.service;

import com.commerceweb.entity.User;
import com.commerceweb.entity.UserRole;
import com.commerceweb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("회원가입 역할 화이트리스트 테스트")
class AuthServiceTest {

    @Mock UserRepository userRepository;

    AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, new BCryptPasswordEncoder(4));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private UserRole savedRoleFor(UserRole requested) {
        authService.signup("a@test.local", "pw", "name", requested);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue().getRole();
    }

    @Test
    @DisplayName("ADMIN으로 가입 요청해도 ADMIN으로 저장되지 않고 CUSTOMER가 된다")
    void adminRequestIsDowngraded() {
        UserRole saved = savedRoleFor(UserRole.ADMIN);

        assertNotEquals(UserRole.ADMIN, saved);
        assertEquals(UserRole.CUSTOMER, saved);
    }

    @Test
    @DisplayName("판매자 가입(SELLER)은 허용한다")
    void sellerIsAllowed() {
        assertEquals(UserRole.SELLER, savedRoleFor(UserRole.SELLER));
    }

    @Test
    @DisplayName("역할을 보내지 않으면 CUSTOMER")
    void missingRoleDefaultsToCustomer() {
        assertEquals(UserRole.CUSTOMER, savedRoleFor(null));
    }

    @Test
    @DisplayName("화이트리스트에 ADMIN이 없다")
    void whitelistNeverContainsAdmin() {
        assertFalse(AuthService.SELF_SIGNUP_ROLES.contains(UserRole.ADMIN));
        for (UserRole role : UserRole.values()) {
            assertNotEquals(UserRole.ADMIN, AuthService.resolveSignupRole(role));
        }
    }
}
