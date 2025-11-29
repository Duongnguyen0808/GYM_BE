INSERT INTO users (fullname, phone_number, email, password, role, is_active, locked, created_at, updated_at)
VALUES 
    ('Admin', '0123456789', NULL, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (phone_number) DO NOTHING;

