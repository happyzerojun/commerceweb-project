package com.commerceweb.config;

import com.commerceweb.entity.User;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.security.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;


    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtProvider.validateToken(token)) {
                try {
                    // 1. 기존에 확실히 존재하는 extractUserId 사용
                    String userIdStr = jwtProvider.extractUserId(token);
                    Long userId = Long.parseLong(userIdStr);

                    User user = userRepository.findById(userId).orElse(null);

                    if (user != null) {
                        // 2. 권한 강제 부여 (ROLE_CUSTOMER)
                        // DB에 CUSTOMER라고 있든 ROLE_CUSTOMER라고 있든 상관없이 시큐리티용 권한 생성
                        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        System.out.println("✅ [DEBUG] 인증 성공: " + user.getEmail() + " (ROLE_CUSTOMER 권한 부여됨)");
                    } else {
                        System.out.println("❌ [DEBUG] DB에 유저 없음 (ID: " + userId + ")");
                    }
                } catch (Exception e) {
                    System.out.println("❌ [DEBUG] 토큰 처리 중 오류: " + e.getMessage());
                }
            } else {
                System.out.println("❌ [DEBUG] 유효하지 않은 토큰");
            }
        }

        filterChain.doFilter(request, response);
    }
}