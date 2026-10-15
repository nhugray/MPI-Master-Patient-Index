package com.mpi.demo.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.mpi.demo.dto.request.LoginRequest;
import com.mpi.demo.dto.request.RegisterRequest;
import com.mpi.demo.dto.response.LoginResponse;
import com.mpi.demo.dto.response.RegisterResponse;
import com.mpi.demo.dto.response.UserResponse;
import com.mpi.demo.entity.RefreshToken;
import com.mpi.demo.entity.User;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.InvalidTokenException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.repository.RefreshTokenRepository;
import com.mpi.demo.repository.UserRepository;
import com.mpi.demo.service.AuthService;

import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", request.email()));
        String rawAccessToken = generateAccessToken(authentication, user.getId());
        String rawRefreshToken = generateRefreshToken(user.getId(), user.getEmail());
        saveRefreshToken(rawRefreshToken, user, deviceInfo, ipAddress);

        log.info("Người dùng đăng nhập thành công: {}", user.getEmail());
        return new LoginResponse(rawAccessToken, rawRefreshToken);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Người dùng", "email", request.email());
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setGender(request.gender());
        user.setAddress(request.address());

        User saved = userRepository.save(user);
        log.info("Người dùng đăng ký thành công: {}", saved.getEmail());
        return RegisterResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public LoginResponse refresh(String rawRefreshToken,String currentIP) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token không hợp lệ"));

        if (storedToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token đã bị thu hồi");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token đã hết hạn");
        }
        

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        if (!storedToken.getIpAddress().equals(currentIP)) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
        throw new InvalidTokenException("IP không khớp - Possible token theft");
        }

        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .toList();

        String newAccessToken = generateAccessTokenFromUser(user.getId(), user.getEmail(), roles);
        String newRefreshToken = generateRefreshToken(user.getId(), user.getEmail());

        saveRefreshToken(newRefreshToken, user, storedToken.getDeviceInfo(), storedToken.getIpAddress());

        log.info("Refresh token được làm mới cho người dùng: {}", user.getEmail());
        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(hashToken(rawRefreshToken))
                .orElseThrow(() -> new InvalidTokenException("Refresh token không hợp lệ"));

        refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
        log.info("Người dùng đăng xuất thành công: {}", storedToken.getUser().getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", email));
        return UserResponse.fromEntity(user);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 không khả dụng", e);
        }
    }

    private String generateAccessToken(Authentication authentication, Long userId) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plusMillis(accessTokenExpiration))
                .claim("userId", userId)
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generateAccessTokenFromUser(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusMillis(accessTokenExpiration))
                .claim("userId", userId)
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generateRefreshToken(Long userId, String email) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusMillis(refreshTokenExpiration))
                .claim("userId", userId)
                .claim("type", "refresh")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void saveRefreshToken(String rawToken, User user, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hashToken(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpiration));
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }


}
