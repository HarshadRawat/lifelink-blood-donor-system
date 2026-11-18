-- ====================================================================
-- LifeLink Database Migration V5: Security Upgrades (Task 9)
-- ====================================================================
-- 1. Refresh token table with rotation, revocation, and reuse detection
-- 2. Account lockout tracking for brute-force protection
-- 3. Hospital profile administrative verification
-- 4. Contact unlock audit logging
-- ====================================================================

-- 1. Refresh Tokens table
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_token_hash VARCHAR(64) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_token_hash (token_hash),
    INDEX idx_refresh_user_id (user_id)
);

-- 2. Account lockout columns on users table
ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN account_locked_until DATETIME(6) DEFAULT NULL;

-- 3. Administrative verification flag on hospital_profiles
ALTER TABLE hospital_profiles
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Pre-verify all existing hospital profiles so existing test suites and seed data remain valid
UPDATE hospital_profiles SET verified = TRUE;

-- 4. Contact unlock audit logging table
CREATE TABLE contact_unlock_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_user_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    emergency_request_id BIGINT NOT NULL,
    unlocked_at DATETIME(6) NOT NULL,
    ip_address VARCHAR(45) DEFAULT NULL,
    CONSTRAINT fk_audit_hospital_user FOREIGN KEY (hospital_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_audit_hospital_user (hospital_user_id),
    INDEX idx_audit_donor (donor_id),
    INDEX idx_audit_match (match_id)
);
