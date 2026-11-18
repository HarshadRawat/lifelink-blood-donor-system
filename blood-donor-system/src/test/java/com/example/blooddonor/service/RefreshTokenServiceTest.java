package com.example.blooddonor.service;

import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.entity.RefreshToken;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.InvalidCredentialsException;
import com.example.blooddonor.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private Clock fixedClock;
    private RefreshTokenService refreshTokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneId.of("UTC"));
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtService, fixedClock);

        testUser = new User();
        testUser.setId(42L);
        testUser.setName("Security User");
        testUser.setEmail("security@example.com");
        testUser.setRole(Role.DONOR);
    }

    @Test
    @DisplayName("createRefreshToken should persist hashed token with 7-day expiration and return raw token string")
    void createRefreshToken_shouldPersistHashedTokenAndReturnRawString() {
        String rawToken = refreshTokenService.createRefreshToken(testUser);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertEquals(RefreshTokenService.hashToken(rawToken), saved.getTokenHash());
        assertFalse(saved.getRevoked());
        assertEquals(LocalDateTime.now(fixedClock).plusDays(7), saved.getExpiresAt());
    }

    @Test
    @DisplayName("rotateRefreshToken should invalidate old token, issue new tokens, and return LoginResponse")
    void rotateRefreshToken_shouldRotateAndReturnNewAccessAndRefreshToken() {
        String oldRawToken = "valid-old-refresh-token";
        String oldHash = RefreshTokenService.hashToken(oldRawToken);

        RefreshToken existing = new RefreshToken();
        existing.setId(1L);
        existing.setUser(testUser);
        existing.setTokenHash(oldHash);
        existing.setExpiresAt(LocalDateTime.now(fixedClock).plusDays(5));
        existing.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(42L, "DONOR")).thenReturn("new-access-jwt");

        LoginResponse response = refreshTokenService.rotateRefreshToken(oldRawToken);

        assertNotNull(response);
        assertEquals("new-access-jwt", response.getToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(oldRawToken, response.getRefreshToken());
        assertEquals(testUser.getEmail(), response.getUser().getEmail());

        // Verify old token marked revoked and replacedByTokenHash set
        assertTrue(existing.getRevoked());
        assertNotNull(existing.getReplacedByTokenHash());

        // Verify two saves: old token updated, new token saved
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotateRefreshToken should throw exception when token is null or blank")
    void rotateRefreshToken_shouldThrowWhenTokenBlankOrNull() {
        assertThrows(InvalidCredentialsException.class, () -> refreshTokenService.rotateRefreshToken(null));
        assertThrows(InvalidCredentialsException.class, () -> refreshTokenService.rotateRefreshToken("  "));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("rotateRefreshToken should throw exception when token hash not found")
    void rotateRefreshToken_shouldThrowWhenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> refreshTokenService.rotateRefreshToken("nonexistent-token"));
    }

    @Test
    @DisplayName("rotateRefreshToken should revoke token and throw when token is expired")
    void rotateRefreshToken_whenExpired_shouldRevokeAndThrow() {
        String rawToken = "expired-token";
        String tokenHash = RefreshTokenService.hashToken(rawToken);

        RefreshToken existing = new RefreshToken();
        existing.setUser(testUser);
        existing.setTokenHash(tokenHash);
        existing.setExpiresAt(LocalDateTime.now(fixedClock).minusHours(1)); // Expired
        existing.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existing));

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenService.rotateRefreshToken(rawToken)
        );

        assertTrue(ex.getMessage().contains("expired"));
        assertTrue(existing.getRevoked());
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    @DisplayName("rotateRefreshToken with revoked token MUST trigger reuse detection, revoke ALL user sessions, and reject")
    void rotateRefreshToken_whenRevoked_shouldTriggerReuseDetectionAndRevokeAllSessions() {
        String compromisedRawToken = "stolen-compromised-token";
        String compromisedHash = RefreshTokenService.hashToken(compromisedRawToken);

        RefreshToken compromisedRecord = new RefreshToken();
        compromisedRecord.setUser(testUser);
        compromisedRecord.setTokenHash(compromisedHash);
        compromisedRecord.setExpiresAt(LocalDateTime.now(fixedClock).plusDays(3));
        compromisedRecord.setRevoked(true); // Already revoked!

        when(refreshTokenRepository.findByTokenHash(compromisedHash)).thenReturn(Optional.of(compromisedRecord));

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenService.rotateRefreshToken(compromisedRawToken)
        );

        assertTrue(ex.getMessage().contains("Compromised refresh token detected"));
        // Token Family Invalidation: all sessions terminated
        verify(refreshTokenRepository).revokeAllForUser(testUser);
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("revokeToken should set revoked true on user logout")
    void revokeToken_shouldMarkTokenRevokedOnLogout() {
        String rawToken = "logout-token";
        String tokenHash = RefreshTokenService.hashToken(rawToken);

        RefreshToken token = new RefreshToken();
        token.setUser(testUser);
        token.setTokenHash(tokenHash);
        token.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken(rawToken);

        assertTrue(token.getRevoked());
        verify(refreshTokenRepository).save(token);
    }
}
