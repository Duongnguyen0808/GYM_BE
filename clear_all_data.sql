SET session_replication_role = 'replica';

DELETE FROM check_in_logs;
DELETE FROM pt_session_logs;
DELETE FROM pt_bookings;
DELETE FROM pt_schedules;
DELETE FROM sale_details;
DELETE FROM transactions;
DELETE FROM pending_renewals;
DELETE FROM pending_upgrades;
DELETE FROM member_packages;
DELETE FROM work_schedules;
DELETE FROM staff_attendance;
DELETE FROM sales;
DELETE FROM members;
DELETE FROM products;
DELETE FROM packages;
DELETE FROM promotions;
DELETE FROM users WHERE phone_number != '0123456789';

SET session_replication_role = 'origin';

INSERT INTO users (fullname, phone_number, email, password, role, is_active, locked, created_at, updated_at)
VALUES 
    ('Admin', '0123456789', NULL, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (phone_number) DO UPDATE
SET 
    fullname = 'Admin',
    email = NULL,
    password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    role = 'ADMIN',
    is_active = true,
    locked = false,
    updated_at = CURRENT_TIMESTAMP;

