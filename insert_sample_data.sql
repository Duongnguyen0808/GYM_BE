-- ============================================
-- FILE SQL ĐỂ CHÈN DỮ LIỆU MẪU VÀO POSTGRESQL
-- Chạy file này trong pgAdmin hoặc psql
-- ============================================

-- 1. INSERT USERS (1 Admin, 1 PT, 2 Members)
-- Password: password (hash BCrypt)
INSERT INTO users (fullname, phone_number, email, password, role, is_active, locked, created_at, updated_at)
VALUES 
    ('admin', '0123456789', 'admin@gym.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PT Nguyễn Văn A', '0987654321', 'pt1@gym.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PT', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Hội viên Phạm Văn D', '0987654324', 'member1@gym.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'MEMBER', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Hội viên Hoàng Thị E', '0987654325', 'member2@gym.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'MEMBER', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (phone_number) DO NOTHING;

-- 2. INSERT MEMBERS (2 học viên)
INSERT INTO members (fullname, phone_number, email, birth_date, address, barcode, user_account_id, created_at, updated_at)
SELECT 
    'Hội viên Phạm Văn D',
    '0987654324',
    'member1@gym.com',
    '1995-05-15'::DATE,
    '123 Đường ABC, Quận 1, TP.HCM',
    '0987654324',
    (SELECT id FROM users WHERE phone_number = '0987654324' LIMIT 1),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM members WHERE phone_number = '0987654324');

INSERT INTO members (fullname, phone_number, email, birth_date, address, barcode, user_account_id, created_at, updated_at)
SELECT 
    'Hội viên Hoàng Thị E',
    '0987654325',
    'member2@gym.com',
    '1998-08-20'::DATE,
    '456 Đường XYZ, Quận 2, TP.HCM',
    '0987654325',
    (SELECT id FROM users WHERE phone_number = '0987654325' LIMIT 1),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM members WHERE phone_number = '0987654325');

-- 3. INSERT GYM PACKAGES (2 gói tập)
INSERT INTO packages (name, description, price, package_type, duration_days, duration_months, number_of_sessions, is_active, allowed_weekdays, created_at, updated_at)
VALUES 
    ('Gói 1 tháng PREMIUM', 'Gói tập 1 tháng không giới hạn lượt', 2000000, 'GYM_ACCESS', NULL, 1, NULL, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Gói PT 20 buổi', 'Gói tập PT 20 buổi, 3 tháng', 5000000, 'PT_SESSION', NULL, 3, 20, true, 'MON,WED,FRI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 4. INSERT MEMBER PACKAGES (2 subscriptions)
INSERT INTO member_packages (member_id, package_id, start_date, end_date, status, remaining_sessions, assigned_pt_id, allowed_weekdays, time_slot)
SELECT 
    m.id,
    p.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 month',
    'ACTIVE',
    NULL,
    NULL,
    NULL,
    NULL
FROM members m
CROSS JOIN packages p
WHERE m.phone_number = '0987654324' AND p.name = 'Gói 1 tháng PREMIUM'
  AND NOT EXISTS (
    SELECT 1 FROM member_packages mp 
    WHERE mp.member_id = m.id AND mp.package_id = p.id AND mp.status = 'ACTIVE'
  );

INSERT INTO member_packages (member_id, package_id, start_date, end_date, status, remaining_sessions, assigned_pt_id, allowed_weekdays, time_slot)
SELECT 
    m.id,
    p.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '3 months',
    'ACTIVE',
    15,
    (SELECT id FROM users WHERE phone_number = '0987654321' LIMIT 1),
    'MON,WED,FRI',
    'AFTERNOON_1'
FROM members m
CROSS JOIN packages p
WHERE m.phone_number = '0987654325' AND p.name = 'Gói PT 20 buổi'
  AND NOT EXISTS (
    SELECT 1 FROM member_packages mp 
    WHERE mp.member_id = m.id AND mp.package_id = p.id AND mp.status = 'ACTIVE'
  );

-- 5. INSERT PRODUCTS (2 sản phẩm)
INSERT INTO products (name, price, stock_quantity, is_active, created_at, updated_at)
VALUES 
    ('Nước uống thể thao', 25000, 100, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Protein Bar', 50000, 50, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- THÔNG TIN ĐĂNG NHẬP:
-- Admin: SĐT: 0123456789, Password: password
-- PT: SĐT: 0987654321, Password: password
-- Member: SĐT: 0987654324 hoặc 0987654325, Password: password
-- ============================================

