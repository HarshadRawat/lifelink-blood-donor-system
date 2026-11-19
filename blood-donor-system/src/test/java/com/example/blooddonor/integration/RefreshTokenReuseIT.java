package com.example.blooddonor.integration;

import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.entity.RefreshToken;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.InvalidCredentialsException;
import com.example.blooddonor.repository.RefreshTokenRepository;
import com.example.blooddonor.repository.UserRepository;
import com.example.blooddonor.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Refresh Token Family Revocation & Reuse Detection Integration Test")
class RefreshTokenReuseIT extends AbstractMySqlIT {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Reuse of old refresh token MUST revoke all user tokens in database and reject subsequent tokens")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reuseRevokedRefreshToken_shouldPersistRevocationOfAllUserTokens() {
        // 1. Create a persistent test user
        User user = new User();
        user.setName("Token Reuse Test User");
        user.setEmail("reuse.test." + System.currentTimeMillis() + "@lifelink.com");
        user.setPassword(passwordEncoder.encode("SecurePass123!"));
        user.setRole(Role.DONOR);
        final User savedUser = userRepository.save(user);
        final Long userId = savedUser.getId();

        // 2. Issue initial refresh token
        String initialToken = refreshTokenService.createRefreshToken(savedUser);
        assertNotNull(initialToken);

        // 3. First rotation: Old token invalidated, new token issued (Legitimate client rotation)
        LoginResponse firstRotation = refreshTokenService.rotateRefreshToken(initialToken);
        String newToken = firstRotation.getRefreshToken();
        assertNotNull(newToken);
        assertNotEquals(initialToken, newToken);

        // 4. Stolen/replayed token scenario: Attacker presents the OLD initial token
        assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenService.rotateRefreshToken(initialToken),
                "Presenting already-revoked token must throw InvalidCredentialsException"
        );

        // 5. Verification: Transaction rollback bug check
        // Because of (noRollbackFor = InvalidCredentialsException.class), the revokeAllForUser MUST be persisted in DB!
        List<RefreshToken> allTokens = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .toList();
        assertFalse(allTokens.isEmpty(), "User must have refresh tokens in DB");
        for (RefreshToken token : allTokens) {
            assertTrue(token.getRevoked(), "Every token for user must have revoked = true after reuse detection");
        }

        // 6. Legitimate client tries to use their new token -> MUST be rejected because entire family was revoked
        assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenService.rotateRefreshToken(newToken),
                "Token from revoked family must be rejected on subsequent presentation"
        );
    }
}
