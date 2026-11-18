-- ====================================================================
-- LifeLink Database Migration V4: Escalation, Cooldown & Request Expiry
-- ====================================================================
-- Supports dynamic radius expansion (20km -> 40km), 90-day donation
-- cooldown enforcement, and automatic stale request expiry (Task 8).
-- ====================================================================

-- 1. Add current_radius_km and expires_at to emergency_requests
ALTER TABLE emergency_requests
    ADD COLUMN current_radius_km DOUBLE NOT NULL DEFAULT 20.0,
    ADD COLUMN expires_at DATETIME(6) DEFAULT NULL;

-- 2. Add last_donation_date to donor_profiles
ALTER TABLE donor_profiles
    ADD COLUMN last_donation_date DATETIME(6) DEFAULT NULL;

-- 3. Composite indexes for scheduled escalation, expiration scans, and cooldown checks
CREATE INDEX idx_emergency_requests_status_created ON emergency_requests (status, created_at);
CREATE INDEX idx_emergency_requests_status_expires ON emergency_requests (status, expires_at);
CREATE INDEX idx_donor_profiles_last_donation ON donor_profiles (last_donation_date);
