package com.example.blooddonor.repository;

import com.example.blooddonor.entity.RefreshToken;
import com.example.blooddonor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllForUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);
}
