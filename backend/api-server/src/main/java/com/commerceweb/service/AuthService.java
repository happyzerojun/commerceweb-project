package com.commerceweb.service;

import com.commerceweb.entity.User;
import com.commerceweb.entity.UserRole;
import com.commerceweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 회원가입으로 스스로 고를 수 있는 역할 화이트리스트.
     * 판매자 가입은 기능상 필요(판매 통계 화면)해서 SELLER만 허용하고, ADMIN은 어떤 요청으로도 만들 수 없다.
     * 이전에는 요청 본문의 role을 그대로 저장해 {"role":"ADMIN"} 한 줄로 관리자 계정을 만들 수 있었다(권한 상승).
     * 관리자 계정은 운영자가 DB에서 직접 지정한다.
     */
    static final Set<UserRole> SELF_SIGNUP_ROLES = EnumSet.of(UserRole.CUSTOMER, UserRole.SELLER);

    /**
     * ✅ 회원가입 로직
     * @param requestedRole 클라이언트가 보낸 역할. 신뢰하지 않고 화이트리스트로 걸러 저장한다 (CUSTOMER/SELLER 외에는 CUSTOMER)
     */
    @Transactional
    public User signup(String email, String rawPassword, String name, UserRole requestedRole) {

        // 1️⃣ 이메일 중복 확인
        if (userRepository.existsByEmail(email)) {
            log.error("회원가입 실패: 이미 존재하는 이메일 -> {}", email);
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        // 2️⃣ 비밀번호 암호화 (BCrypt)
        String hashedPassword = passwordEncoder.encode(rawPassword);
        log.info("비밀번호 암호화 완료");

        // 3️⃣ User 객체 생성
        User user = new User();
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setName(name);

        // 4️⃣ 역할 설정: 요청 값은 화이트리스트(CUSTOMER/SELLER)에 있을 때만 사용, 그 외(ADMIN 등)는 일반 사용자
        user.setRole(resolveSignupRole(requestedRole));
        if (requestedRole != null && requestedRole != user.getRole()) {
            log.warn("회원가입 역할 요청 무시: email={}, requested={}, saved={}", email, requestedRole, user.getRole());
        }

        // 5️⃣ 데이터베이스에 저장
        User savedUser = userRepository.save(user);

        log.info("✅ 회원가입 성공 및 DB 저장 완료: {}, Role: {}", savedUser.getEmail(), savedUser.getRole());
        return savedUser;
    }

    static UserRole resolveSignupRole(UserRole requestedRole) {
        return requestedRole != null && SELF_SIGNUP_ROLES.contains(requestedRole) ? requestedRole : UserRole.CUSTOMER;
    }

    /**
     * ✅ 로그인 로직
     */
    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        // 1️⃣ 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        // 2️⃣ 입력한 비밀번호와 저장된 비밀번호 비교
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("❌ 로그인 실패: 비밀번호 불일치 -> {}", email);
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        log.info("✅ 로그인 성공: {}", user.getEmail());
        return user;
    }
}