package com.commerceweb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

/**
 * Spring Security 설정 클래스
 * - 보안 필터 체인 설정 (CORS, CSRF, 세션 정책 등)
 * - URL별 접근 권한 설정
 * - JWT 인증 필터 등록
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter; // 우리가 만든 커스텀 JWT 필터

    /**
     * 인증 매니저(AuthenticationManager) 빈 등록
     * - 로그인 시 사용자 인증(아이디/비번 검증)을 담당하는 핵심 컴포넌트
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 보안 필터 체인(SecurityFilterChain) 설정
     * - HTTP 요청에 대한 보안 규칙을 정의함
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (REST API는 Stateless하므로 불필요, 403 에러 방지)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 설정 적용 (React 프론트엔드와의 통신 허용)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 기본 로그인 폼 비활성화 (JWT 방식이므로 필요 없음)
                .formLogin(AbstractHttpConfigurer::disable)

                // 4. HTTP Basic 인증 비활성화 (보안에 취약하므로 JWT 사용)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 5. 세션 관리 정책 설정: STATELESS (서버에 세션을 저장하지 않음, JWT 필수 설정)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 6. URL별 접근 권한 설정 (Authorization)
                .authorizeHttpRequests(auth -> auth
                        // ✅ CORS Preflight 요청(OPTIONS)은 무조건 허용 (브라우저 정책)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ 에러 페이지 접근 허용
                        .requestMatchers("/error").permitAll()

                        // ✅ 인증 없이 접근 가능한 공개 API (로그인, 회원가입, 상품 조회 등)
                        .requestMatchers("/signup").permitAll() // 회원가입
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll() // 상품 목록/상세 조회
                        .requestMatchers("/api/auth/**").permitAll() // 로그인/회원가입 API
                        .requestMatchers("/api/sales/**").permitAll() // 판매 관련 (추후 권한 조정 가능)
                        .requestMatchers("/api/reviews/**").permitAll() // 리뷰 조회 (작성은 컨트롤러 내부에서 체크)

                        // ✅ 인증된 사용자만 접근 가능한 API (주문 등)
                        // hasAnyRole: "ROLE_" 접두사가 자동으로 붙어서 체크됨 (예: ROLE_CUSTOMER)
                        // 💥 중요: SELLER 권한도 주문 API를 쓸 수 있도록 추가함 (403 해결)
                        .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "USER", "ADMIN", "SELLER")

                        // ✅ 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 7. JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                // - Spring Security의 기본 인증보다 먼저 JWT를 검사해서 로그인 처리함
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 암호화 인코더 빈 등록
     * - BCrypt 알고리즘 사용 (단방향 암호화)
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * CORS 설정 (Cross-Origin Resource Sharing)
     * - 다른 도메인(React)에서 API를 호출할 수 있도록 허용하는 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 쿠키나 인증 헤더(Authorization)를 포함한 요청 허용
        config.setAllowCredentials(true);

        // ✅ 허용할 오리진(프론트엔드 도메인) 목록
        // 로컬 개발 환경, Docker 환경, 배포 서버 IP 등 모두 포함
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost",       // 기본
                "http://localhost:80",    // Docker Front
                "http://localhost:3000",  // React Dev
                "http://localhost:8080",  // Backend
                "http://127.0.0.1:80",
                "http://127.0.0.1:8080",
                "http://13.236.117.206",  // AWS 배포 IP
                "http://13.236.117.206:80",
                "http://13.236.117.206:8080"
        ));

        // 허용할 HTTP 메서드 (Wildcard * 지양, 명시적 허용)
        config.addAllowedMethod(HttpMethod.GET);
        config.addAllowedMethod(HttpMethod.POST);
        config.addAllowedMethod(HttpMethod.PUT);
        config.addAllowedMethod(HttpMethod.PATCH);
        config.addAllowedMethod(HttpMethod.DELETE);
        config.addAllowedMethod(HttpMethod.OPTIONS); // Preflight 필수

        // 허용할 HTTP 헤더
        config.addAllowedHeader("Authorization");   // JWT 토큰
        config.addAllowedHeader("Content-Type");    // JSON
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Origin");

        // Preflight 요청 캐시 시간 (1시간)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 모든 경로에 대해 위 설정 적용
        return source;
    }
}
