package com.example.blooddonor.service;

import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.dto.UserResponse;
import com.example.blooddonor.entity.RefreshToken;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.InvalidCredentialsException;
import com.example.blooddonor.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays = 7;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * Issues a new refresh token for the user and stores its SHA-256 hash in the database.
     *
     * @param user authenticated user
     * @return raw opaque refresh token string
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusDays(refreshTokenExpirationDays);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(now);

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Rotates an existing refresh token with reuse detection.
     * If an already revoked token is used, all tokens for the user are revoked immediately (compromise detection).
     *
     * @param rawRefreshToken incoming refresh token
     * @return LoginResponse containing new access token, new refresh token, and user details
     */
    @Transactional
    public LoginResponse rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException("Refresh token must not be empty");
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken tokenRecord = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        User user = tokenRecord.getUser();
        LocalDateTime now = LocalDateTime.now(clock);

        // Reuse Detection: A revoked token being presented indicates potential token theft
        if (Boolean.TRUE.equals(tokenRecord.getRevoked())) {
            log.warn("SECURITY ALERT: Reuse of revoked refresh token detected for userId={}. Revoking all sessions.", user.getId());
            refreshTokenRepository.revokeAllForUser(user);
            throw new InvalidCredentialsException("Compromised refresh token detected. All sessions revoked.");
        }

        // Expiry Detection
        if (tokenRecord.isExpired(now)) {
            tokenRecord.setRevoked(true);
            refreshTokenRepository.save(tokenRecord);
            throw new InvalidCredentialsException("Refresh token has expired. Please login again.");
        }

        // 1. Invalidate current token (Rotation)
        tokenRecord.setRevoked(true);

        // 2. Issue new refresh token
        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);

        tokenRecord.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(tokenRecord);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(newTokenHash);
        newRefreshToken.setExpiresAt(now.plusDays(refreshTokenExpirationDays));
        newRefreshToken.setRevoked(false);
        newRefreshToken.setCreatedAt(now);
        refreshTokenRepository.save(newRefreshToken);

        // 3. Issue new short-lived access token
        String newAccessToken = jwtService.generateToken(user.getId(), user.getRole().name());

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setName(user.getName());
        userResponse.setEmail(user.getEmail());
        userResponse.setRole(user.getRole().name());

        LoginResponse response = new LoginResponse();
        response.setToken(newAccessToken);
        response.setRefreshToken(newRawToken);
        response.setUser(userResponse);

        return response;
    }

    /**
     * Revokes a refresh token on user logout.
     *
     * @param rawRefreshToken token to revoke
     */
    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
