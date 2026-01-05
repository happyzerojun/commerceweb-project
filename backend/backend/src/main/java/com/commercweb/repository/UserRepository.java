package com.commercweb.repository;

import com.commercweb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ email로 사용자 조회
    Optional<User> findByEmail(String email);

    // ✅ email이 이미 존재하는지 확인
    boolean existsByEmail(String email);
}