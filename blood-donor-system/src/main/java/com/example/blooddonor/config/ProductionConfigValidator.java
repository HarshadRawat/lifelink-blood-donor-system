package com.example.blooddonor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("prod")
public class ProductionConfigValidator {

    private static final Set<String> KNOWN_BAD = Set.of(
            "my-super-secret-key-for-blood-donor-system-12345",
            "blood_donor_super_secret_jwt_key_minimum_32_characters_long",
            "Admin123", "Admin123!", "rootpassword", "guest"
    );

    public ProductionConfigValidator(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration-ms}") long accessTtlMs,
            @Value("${app.admin.password:}") String adminPassword) {
        if (jwtSecret == null || jwtSecret.length() < 32 || KNOWN_BAD.contains(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET missing, too short or a known default");
        }
        if (accessTtlMs > 3_600_000L) {
            throw new IllegalStateException("Access token TTL must be 1 hour or less");
        }
        if (adminPassword != null && !adminPassword.isBlank() && (adminPassword.length() < 12 || KNOWN_BAD.contains(adminPassword))) {
            throw new IllegalStateException("ADMIN_PASSWORD is weak or a known default");
        }
    }
}
