-- V6: Add last_escalated_at column for escalation cadence tracking and add EXPIRED to status ENUM
ALTER TABLE emergency_requests ADD COLUMN last_escalated_at DATETIME(6) NULL;

ALTER TABLE emergency_requests
    MODIFY COLUMN status ENUM('ACTIVE','CANCELLED','FULFILLED','EXPIRED') DEFAULT NULL;
