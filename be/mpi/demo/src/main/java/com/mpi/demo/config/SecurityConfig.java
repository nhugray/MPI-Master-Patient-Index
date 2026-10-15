package com.mpi.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpi.demo.helper.ApiResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String REVIEWER = "REVIEWER";
    private static final String VIEWER = "VIEWER";

    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/uploads/**",
            "/error"
    };

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/auth/register").hasRole(ADMIN)

                        // ===== CHỈ ADMIN =====
                        .requestMatchers("/api/v1/users/**", "/api/v1/roles/**").hasRole(ADMIN)
                        .requestMatchers("/api/v1/match-decision-logs/**").hasRole(ADMIN)

                        // ===== FACILITY / SOURCE SYSTEM =====
                        // Rule GET phải đặt TRƯỚC rule chung bên dưới (khớp rule đầu tiên là dừng)
                        .requestMatchers(HttpMethod.GET, "/api/v1/facilities/**", "/api/v1/source-systems/**")
                        .hasAnyRole(ADMIN, REVIEWER, VIEWER)
                        .requestMatchers("/api/v1/facilities/**", "/api/v1/source-systems/**")
                        .hasRole(ADMIN)

                        // ===== PATIENT MASTER (Golden Record) =====
                        // Đọc: cả 3 role. Ghi/merge: chỉ ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/patient-masters/**")
                        .hasAnyRole(ADMIN, REVIEWER, VIEWER)
                        .requestMatchers("/api/v1/patient-masters/**").hasRole(ADMIN)

                        .requestMatchers("/api/v1/import/**").hasAnyRole(ADMIN, REVIEWER)
                        .requestMatchers("/api/v1/patients/**").hasAnyRole(ADMIN, REVIEWER)
                        .requestMatchers("/api/v1/match-candidates/**").hasAnyRole(ADMIN, REVIEWER)

                        .requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**")
                        .hasAnyRole(ADMIN, REVIEWER, VIEWER)

                        .anyRequest().denyAll())

                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> body = ApiResponse.forbidden("Bạn không có quyền truy cập tài nguyên này");
            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(body));
        };
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> body = ApiResponse.unauthorized("Token không hợp lệ hoặc đã hết hạn");
            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(body));
        };
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}