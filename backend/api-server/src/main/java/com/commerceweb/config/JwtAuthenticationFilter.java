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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 인증 필터
 * - 클라이언트 요청 헤더에서 JWT 토큰을 추출하고 검증합니다.
 * - 검증 성공 시, 해당 사용자를 인증된 상태로 SecurityContext에 등록합니다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    /**
     * 필터 로직 수행 (모든 요청마다 실행됨)
     */
    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Authorization 헤더 추출
        String authHeader = request.getHeader("Authorization");

        // 2. 헤더가 존재하고 "Bearer "로 시작하는지 확인
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // "Bearer " 제거 후 순수 토큰만 추출

            // 3. JWT 토큰 유효성 검증
            if (jwtProvider.validateToken(token)) {
                try {
                    // 4. 토큰에서 사용자 ID(PK) 추출
                    String userIdStr = jwtProvider.extractUserId(token);
                    Long userId = Long.parseLong(userIdStr);

                    // 5. DB에서 사용자 조회 (토큰 위조 방지 및 최신 정보 로드)
                    User user = userRepository.findById(userId).orElse(null);

                    if (user != null) {
                        // ✅ [핵심 로직] DB User 엔티티를 Spring Security 표준인 UserDetails로 변환
                        // 이 과정에서 권한 이름 포맷(ROLE_XXX)을 맞추어 403 에러를 방지함
                        UserDetails userDetails = createSpringSecurityUser(user);

                        // 6. 인증 토큰 생성 (Principal에 UserDetails 객체 저장)
                        // - Principal: UserDetails (컨트롤러에서 @AuthenticationPrincipal로 받을 객체)
                        // - Credentials: null (이미 JWT로 인증했으므로 비밀번호 필요 없음)
                        // - Authorities: 권한 목록 (ROLE_CUSTOMER, ROLE_SELLER 등)
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                        // 7. 요청의 부가 정보(IP 등) 설정
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 8. SecurityContext에 인증 객체 저장 -> "로그인 성공" 처리 완료
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    // 토큰 파싱이나 DB 조회 중 에러 발생 시 로그만 남기고 다음 필터로 진행 (인증 실패 상태 유지)
                    logger.error("Could not set user authentication in security context", e);
                }
            }
        }

        // 9. 다음 필터 체인으로 요청 넘김 (필수)
        filterChain.doFilter(request, response);
    }

    /**
     * [Helper Method] DB User 엔티티 -> Spring Security UserDetails 변환
     * 역할:
     * 1. DB의 Role Enum(예: SELLER)을 Spring Security가 인식하는 표준 포맷("ROLE_SELLER")으로 변환
     * 2. SecurityConfig의 .hasRole("SELLER") 설정과 매칭되도록 보장
     * 3. 403 Forbidden 에러의 주원인이었던 권한 이름 불일치 문제를 해결함
     */
    private UserDetails createSpringSecurityUser(User user) {
        // 1. DB에서 Role 가져오기 (없으면 기본값 CUSTOMER)
        String rawRole = user.getRole() != null ? user.getRole().name() : "CUSTOMER";

        // 2. 안전장치: 만약 이미 "ROLE_"이 붙어있다면 제거 (중복 방지: ROLE_ROLE_SELLER 방지)
        if (rawRole.startsWith("ROLE_")) {
            rawRole = rawRole.substring(5);
        }

        // 3. 표준 접두사("ROLE_")를 붙여서 최종 권한 이름 생성
        String finalRoleName = "ROLE_" + rawRole;

        // 4. 권한 객체 리스트 생성
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(finalRoleName));

        // 5. UserDetails 객체 반환
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // username: 컨트롤러에서 Principal.getName()으로 꺼낼 값
                "",              // password: 필요 없음
                authorities      // authorities: 권한 정보
        );
    }
}
