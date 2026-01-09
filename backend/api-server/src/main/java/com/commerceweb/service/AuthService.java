package com.commerceweb.service;

import com.commerceweb.entity.User;
import com.commerceweb.entity.UserRole;
import com.commerceweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * ✅ 회원가입 로직 (role 파라미터 추가 완료)
     */
    @Transactional
    public User signup(String email, String rawPassword, String name, UserRole role) { // ✅ role 인자 추가

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

        // 4️⃣ 전달받은 role 설정 (CUSTOMER 또는 SELLER)
        // ✅ 고정값 CUSTOMER에서 매개변수 role로 변경
        user.setRole(role != null ? role : UserRole.CUSTOMER);

        // 5️⃣ 데이터베이스에 저장
        User savedUser = userRepository.save(user);

        log.info("✅ 회원가입 성공 및 DB 저장 완료: {}, Role: {}", savedUser.getEmail(), savedUser.getRole());
        return savedUser;
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