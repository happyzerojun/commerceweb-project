package com.commerceweb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // ✅ CORS 설정을 가장 먼저 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ CORS 해결을 위해 OPTIONS 요청은 무조건 허용 (Preflight 해결)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 👇 [추가됨] 회원가입(/signup) 요청 허용
                        .requestMatchers("/signup").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/sales/**").permitAll()
                        .requestMatchers("/api/reviews/**").permitAll()
                        .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // ✅ AWS IP와 포트 번호가 포함된 모든 조합을 허용하도록 수정
        // ✅ 수정됨: 프론트엔드(80포트)와 로컬 개발 환경을 확실하게 허용
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost",       // 프론트엔드 (기본 포트 80은 생략 가능하지만 명시)
                "http://localhost:80",    // 프론트엔드 (Docker 80포트)
                "http://localhost:3000",  // 로컬 React 개발용 (혹시 몰라 추가)
                "http://localhost:8080",  // 백엔드 Swagger 등 접근용
                "http://127.0.0.1:80",    // IP로 접근하는 경우
                "http://127.0.0.1:8080",
                "http://13.236.117.206",  // 기존 AWS IP 유지
                "http://13.236.117.206:80",
                "http://13.236.117.206:8080"
        ));

        config.addAllowedMethod("*"); // GET, POST, PUT, DELETE, OPTIONS 모두 허용
        config.addAllowedHeader("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
