-- ====================================================================
-- LifeLink Database Migration V1: Initial Base Schema
-- ====================================================================
-- Matches Hibernate production schema for MySQL 8.0
-- Dependency Order:
--   1. users
--   2. donor_profiles (depends on users)
--   3. hospital_profiles (depends on users)
--   4. emergency_requests (depends on hospital_profiles)
--   5. request_donor_matches (depends on emergency_requests, donor_profiles)
--   6. notifications (depends on users, emergency_requests)
-- ====================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) DEFAULT NULL,
    name VARCHAR(255) DEFAULT NULL,
    password VARCHAR(255) DEFAULT NULL,
    role ENUM('ADMIN', 'DONOR', 'HOSPITAL') DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Donor Profiles Table
CREATE TABLE IF NOT EXISTS donor_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,
    blood_group VARCHAR(255) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    city VARCHAR(255) DEFAULT NULL,
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    contact_number VARCHAR(255) DEFAULT NULL,
    available BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_donor_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_donor_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Hospital Profiles Table
CREATE TABLE IF NOT EXISTS hospital_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,
    hospital_name VARCHAR(255) DEFAULT NULL,
    registration_number VARCHAR(255) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    city VARCHAR(255) DEFAULT NULL,
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    contact_number VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hospital_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_hospital_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Emergency Requests Table
CREATE TABLE IF NOT EXISTS emergency_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hospital_id BIGINT DEFAULT NULL,
    blood_group VARCHAR(255) DEFAULT NULL,
    units_required INT DEFAULT NULL,
    urgency ENUM('CRITICAL', 'HIGH', 'LOW', 'MEDIUM') DEFAULT NULL,
    status ENUM('ACTIVE', 'CANCELLED', 'FULFILLED') DEFAULT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_emergency_requests_hospital FOREIGN KEY (hospital_id) REFERENCES hospital_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Request Donor Matches Table
CREATE TABLE IF NOT EXISTS request_donor_matches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT DEFAULT NULL,
    donor_id BIGINT DEFAULT NULL,
    status ENUM('ACCEPTED', 'COMPLETED', 'MATCHED', 'NOTIFIED', 'REJECTED') DEFAULT NULL,
    created_at DATETIME(6) DEFAULT NULL,
    contact_sharing_consent BIT(1) DEFAULT b'0',
    PRIMARY KEY (id),
    CONSTRAINT fk_request_donor_matches_request FOREIGN KEY (request_id) REFERENCES emergency_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_request_donor_matches_donor FOREIGN KEY (donor_id) REFERENCES donor_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    emergency_request_id BIGINT DEFAULT NULL,
    type ENUM('MATCH_ACCEPTED', 'MATCH_FOUND', 'MATCH_REJECTED', 'REQUEST_CANCELLED', 'REQUEST_FULFILLED') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_emergency_request FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
