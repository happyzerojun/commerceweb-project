package com.commerceweb.config;

import com.commerceweb.entity.User;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.security.jwt.JwtProvider;
import jakarta.annotation.Nonnull;
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

/**
 * JWT 인증 필터: 모든 요청에서 JWT 토큰을 검증하고 사용자를 인증 상태로 설정
 */
@Component  // Spring Bean으로 등록
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;  // JWT 토큰 검증 및 추출
    private final UserRepository userRepository;  // 사용자 DB 조회

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,  // Null 불가
            @Nonnull HttpServletResponse response,  // Null 불가
            @Nonnull FilterChain filterChain  // Null 불가
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");  // Authorization 헤더 추출

        // 이건 Bearer 토큰(JWT) 방식이다"라고 명시, OAuth 2.0 표준이고, 여러 인증 방식을 구분하기 위함
        if (authHeader != null && authHeader.startsWith("Bearer ")) {  // 토큰 형식 확인
            String token = authHeader.substring(7);  // "Bearer " 제거

            if (jwtProvider.validateToken(token)) {  // JWT 토큰 유효성 검증
                try {
                    String userIdStr = jwtProvider.extractUserId(token);  // 토큰에서 userId 추출
                    Long userId = Long.parseLong(userIdStr);  // String → Long 변환

                    User user = userRepository.findById(userId).orElse(null);  // DB에서 사용자 조회

                    if (user != null) {  // 사용자가 존재하면
                        SecurityContextHolder.getContext().setAuthentication(
                                createAuthenticationWithDetails(user, request)  // 인증 토큰 생성 + 설정 + 저장
                        );

                        System.out.println("✅ [DEBUG] 인증 성공: " + user.getEmail());
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

        filterChain.doFilter(request, response);  // 다음 필터로 요청 전달
    }

    /**
     * 인증 토큰 생성 + 요청 정보 설정 (메서드 추출)
     * @param user 사용자 정보
     * @param request HTTP 요청 객체
     * @return 설정이 완료된 인증 토큰
     */
    private static UsernamePasswordAuthenticationToken createAuthenticationWithDetails(
            User user,  // 사용자 정보
            HttpServletRequest request  // HTTP 요청
    ) {
        // "ROLE_" 접두사가 필수 (Spring Security 규칙) 실제 DB의 user.getRole() = "CUSTOMER"라고 되어있으면, 여기서 "ROLE_CUSTOMER"로 변환하는 것
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));  // 권한 생성

        // 이 사람이 누구이고, 어떤 권한을 가지고 있는가
        //     *   1. principal (주체): 사용자 식별 정보
        //     *   2. credentials (자격증명): 비밀번호
        //     *   3. authorities (권한): 할 수 있는 것들
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(  // 인증 토큰 생성
                user.getEmail(),  // principal: 사용자 이메일
                null,  // credentials: JWT이므로 null
                authorities  // 권한 리스트
                // 비밀번호는 최대한 적게 다루어야 함 null로 지정함으로서 실수로 비밀번호를 출력하거나, 직렬화해서 외부로 내보낼 위험이 줄어듦
        );

        token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));  // 요청 상세 정보 설정

        return token;  // 설정 완료된 토큰 반환
    }
}
