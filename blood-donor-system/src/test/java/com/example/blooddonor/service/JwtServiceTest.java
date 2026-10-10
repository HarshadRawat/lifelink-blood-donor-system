package com.example.blooddonor.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();


    // =========================================================
    // 1. TOKEN GENERATION
    // =========================================================

    @Test
    void generateToken_shouldReturnToken() {

        String token =
                jwtService.generateToken(9L, "DONOR");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }


    // =========================================================
    // 2. TOKEN VALIDATION
    // =========================================================

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {

        String token =
                jwtService.generateToken(9L, "DONOR");

        assertTrue(
                jwtService.isTokenValid(token)
        );
    }


    // =========================================================
    // 3. EXTRACT USER ID
    // =========================================================

    @Test
    void extractUserId_shouldReturnCorrectUserId() {

        String token =
                jwtService.generateToken(9L, "DONOR");

        Long userId =
                jwtService.extractUserId(token);

        assertEquals(9L, userId);
    }


    // =========================================================
    // 4. EXTRACT ROLE
    // =========================================================

    @Test
    void extractRole_shouldReturnCorrectRole() {

        String token =
                jwtService.generateToken(9L, "DONOR");

        String role =
                jwtService.extractRole(token);

        assertEquals("DONOR", role);
    }


    // =========================================================
    // 5. TAMPERED TOKEN
    // =========================================================

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsTampered() {

        String token =
                jwtService.generateToken(9L, "DONOR");

        String tamperedToken =
                token.substring(0, token.length() - 1) + "X";

        assertFalse(
                jwtService.isTokenValid(tamperedToken)
        );
    }


    // =========================================================
    // 6. COMPLETELY INVALID TOKEN
    // =========================================================

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsInvalid() {

        String invalidToken =
                "this-is-not-a-valid-jwt";

        assertFalse(
                jwtService.isTokenValid(invalidToken)
        );
    }

    @Test
    void generateExpiredTokenForTesting() {
        String token = jwtService.generateExpiredToken(8L, "HOSPITAL");
        System.out.println("EXPIRED_TEST_TOKEN=" + token);
    }
}