package com.example.blooddonor.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductionConfigValidator Unit Tests")
class ProductionConfigValidatorTest {

    private static final String VALID_SECRET = "a-very-strong-production-jwt-secret-with-plenty-of-entropy-123456";
    private static final long VALID_TTL = 900000L; // 15 minutes
    private static final String VALID_ADMIN_PASS = "SuperSecureAdminPassword2026!";

    @Test
    @DisplayName("Should pass when all production secrets and TTL are valid")
    void shouldPassWithValidSecrets() {
        assertDoesNotThrow(() -> new ProductionConfigValidator(VALID_SECRET, VALID_TTL, VALID_ADMIN_PASS));
    }

    @Test
    @DisplayName("Should pass when admin password is blank (admin creation disabled)")
    void shouldPassWithBlankAdminPassword() {
        assertDoesNotThrow(() -> new ProductionConfigValidator(VALID_SECRET, VALID_TTL, ""));
        assertDoesNotThrow(() -> new ProductionConfigValidator(VALID_SECRET, VALID_TTL, null));
    }

    @Test
    @DisplayName("Should throw when JWT_SECRET is too short (< 32 characters)")
    void shouldThrowWhenJwtSecretTooShort() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new ProductionConfigValidator("short-secret-key", VALID_TTL, VALID_ADMIN_PASS)
        );
        assertTrue(ex.getMessage().contains("JWT_SECRET missing, too short or a known default"));
    }

    @Test
    @DisplayName("Should throw when JWT_SECRET is a known default")
    void shouldThrowWhenJwtSecretIsKnownBad() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new ProductionConfigValidator("my-super-secret-key-for-blood-donor-system-12345", VALID_TTL, VALID_ADMIN_PASS)
        );
        assertTrue(ex.getMessage().contains("JWT_SECRET missing, too short or a known default"));
    }

    @Test
    @DisplayName("Should throw when Access Token TTL is greater than 1 hour")
    void shouldThrowWhenTtlTooLong() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new ProductionConfigValidator(VALID_SECRET, 3_600_001L, VALID_ADMIN_PASS)
        );
        assertTrue(ex.getMessage().contains("Access token TTL must be 1 hour or less"));
    }

    @Test
    @DisplayName("Should throw when ADMIN_PASSWORD is weak or too short")
    void shouldThrowWhenAdminPasswordTooShort() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new ProductionConfigValidator(VALID_SECRET, VALID_TTL, "Pass1!")
        );
        assertTrue(ex.getMessage().contains("ADMIN_PASSWORD is weak or a known default"));
    }

    @Test
    @DisplayName("Should throw when ADMIN_PASSWORD is a known default")
    void shouldThrowWhenAdminPasswordIsKnownBad() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new ProductionConfigValidator(VALID_SECRET, VALID_TTL, "Admin123")
        );
        assertTrue(ex.getMessage().contains("ADMIN_PASSWORD is weak or a known default"));
    }
}
