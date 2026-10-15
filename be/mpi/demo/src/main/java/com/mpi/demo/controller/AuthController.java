package com.mpi.demo.controller;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.LoginRequest;
import com.mpi.demo.dto.request.RefreshRequest;
import com.mpi.demo.dto.request.RegisterRequest;
import com.mpi.demo.dto.response.LoginResponse;
import com.mpi.demo.dto.response.RegisterResponse;
import com.mpi.demo.dto.response.UserResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final int COOKIE_MAX_AGE = 259200;

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký thành công", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = extractClientIp(httpRequest);

        LoginResponse loginResponse = authService.login(request, deviceInfo, ipAddress);
        setRefreshTokenCookie(httpResponse, loginResponse.refreshToken());

        return ResponseEntity.ok(ApiResponse.success("Token đã được làm mới", loginResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String rawRefreshToken = extractRefreshToken(body, httpRequest);
        String currentIP = extractClientIp(httpRequest);

        LoginResponse loginResponse = authService.refresh(rawRefreshToken, currentIP);
        setRefreshTokenCookie(httpResponse, loginResponse.refreshToken());

        return ResponseEntity.ok(ApiResponse.success("Token đã được làm mới",
                loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String rawRefreshToken = extractRefreshTokenFromCookie(httpRequest);
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            authService.logout(rawRefreshToken);
        }
        clearRefreshTokenCookie(httpResponse);

        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getSubject();
        UserResponse userResponse = authService.getMe(email);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String rawToken) {
        response.setHeader("Set-Cookie",
                REFRESH_TOKEN_COOKIE_NAME + "=" + rawToken
                        + "; HttpOnly; Secure; SameSite=Lax; Path=" + COOKIE_PATH
                        + "; Max-Age=" + COOKIE_MAX_AGE);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        response.setHeader("Set-Cookie", REFRESH_TOKEN_COOKIE_NAME + "=; Max-Age=0; Path=" + COOKIE_PATH);
    }

    private String extractRefreshToken(RefreshRequest body, HttpServletRequest request) {
        String fromCookie = extractRefreshTokenFromCookie(request);
        if (fromCookie != null) {
            return fromCookie;
        }
        if (body != null && body.refreshToken() != null &&
                !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        return null;
    }

}
