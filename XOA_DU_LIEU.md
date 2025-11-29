# 🗑️ XÓA HẾT DỮ LIỆU, CHỈ GIỮ LẠI ADMIN

## Cách 1: Dùng Script SQL (Khuyến nghị)

```bash
# Chạy script SQL
psql -U postgres -d gym_management_db -f BE/clear_all_data.sql

# Hoặc mở pgAdmin và chạy file: BE/clear_all_data.sql
```

## Cách 2: Chạy trực tiếp trong PostgreSQL

```sql
-- Kết nối database
\c gym_management_db

-- Tắt foreign key checks tạm thời
SET session_replication_role = 'replica';

-- Xóa các bảng con trước
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

-- Xóa các bảng cha
DELETE FROM sales;
DELETE FROM members;
DELETE FROM products;
DELETE FROM packages;
DELETE FROM promotions;

-- Xóa tất cả users TRỪ admin
DELETE FROM users WHERE phone_number != '0123456789';

-- Bật lại foreign key checks
SET session_replication_role = 'origin';

-- Đảm bảo có admin
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
```

## Cách 3: Xóa và tạo lại database (Cẩn thận!)

```sql
-- ⚠️ CẢNH BÁO: Cách này sẽ xóa HẾT, kể cả schema
DROP DATABASE gym_management_db;
CREATE DATABASE gym_management_db;

-- Sau đó chạy lại ứng dụng Spring Boot để tạo schema
-- Và chạy create_admin.sql để tạo admin
```

## Kiểm tra sau khi xóa

```sql
-- Kiểm tra số lượng users (chỉ nên có 1 admin)
SELECT COUNT(*) FROM users;
SELECT * FROM users;

-- Kiểm tra các bảng khác (nên = 0)
SELECT COUNT(*) FROM members;
SELECT COUNT(*) FROM packages;
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM member_packages;
```

## Thông tin Admin sau khi xóa

- **SĐT**: `0123456789`
- **Password**: `admin123`
- **Role**: `ADMIN`
- **Status**: `Đang hoạt động`

---

**Lưu ý**: Script sẽ xóa HẾT dữ liệu, chỉ giữ lại admin. Hãy backup database trước nếu cần!


