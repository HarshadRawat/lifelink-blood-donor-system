-- ====================================================================
-- LifeLink Database Migration V2: Performance & Query Optimization Indexes
-- ====================================================================
-- Targets primary filtering paths:
--   1. Donor matching engine: (blood_group, available) and (latitude, longitude)
--   2. Hospital request listings: (status, urgency)
--   3. Match status tracking: (request_id, status) and (donor_id, status)
--   4. Real-time notifications: (user_id, is_read) and (created_at)
-- ====================================================================

-- 1. Matching Engine: Filter active eligible donors by blood group
CREATE INDEX idx_donor_blood_avail ON donor_profiles (blood_group, available);

-- 2. Matching Engine: Geolocation bounding-box pre-filtering
CREATE INDEX idx_donor_lat_lng ON donor_profiles (latitude, longitude);

-- 3. Emergency Requests: Filter requests by operational status and urgency level
CREATE INDEX idx_request_status ON emergency_requests (status, urgency);

-- 4. Match Management: Lookup matches by emergency request and acceptance status
CREATE INDEX idx_match_request ON request_donor_matches (request_id, status);

-- 5. Match Management: Lookup matches by donor and status
CREATE INDEX idx_match_donor ON request_donor_matches (donor_id, status);

-- 6. Notifications: Fast lookup of unread notifications for a specific user
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);

-- 7. Notifications: Chronological notification ordering
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
