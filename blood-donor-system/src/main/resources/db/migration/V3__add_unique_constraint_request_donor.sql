-- ====================================================================
-- LifeLink Database Migration V3: Idempotent Match Unique Constraint
-- ====================================================================
-- Prevents duplicate matching records when emergency request events
-- are re-delivered or retried across asynchronous messaging queues (Task 7).
-- ====================================================================

ALTER TABLE request_donor_matches 
    ADD CONSTRAINT uk_request_donor UNIQUE (request_id, donor_id);
