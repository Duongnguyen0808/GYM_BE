# 🔐 TẠO TÀI KHOẢN ADMIN

## Cách 1: Dùng SQL Script (Nhanh nhất)

```bash
# Chạy script SQL
psql -U postgres -d gym_management_db -f create_admin.sql

# Hoặc mở pgAdmin và chạy file: BE/create_admin.sql
```

**Thông tin đăng nhập:**
- **SĐT**: `0123456789`
- **Password**: `admin123`

---

## Cách 2: Tạo qua Web Interface

1. Đăng nhập với tài khoản admin hiện có
2. Vào: **Quản lý** → **Quản lý người dùng** → **Tạo mới**
3. Điền thông tin:
   - Họ và tên: `Admin`
   - Số điện thoại: `0123456789`
   - Mật khẩu: `admin123`
   - Vai trò: `Quản trị viên (ADMIN)`
4. Nhấn **Tạo tài khoản**

---

## Cách 3: Tạo trực tiếp trong PostgreSQL

```sql
-- Kết nối database
\c gym_management_db

-- Tạo admin (Password: admin123)
INSERT INTO users (fullname, phone_number, email, password, role, is_active, locked, created_at, updated_at)
VALUES 
    ('Admin', '0123456789', NULL, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (phone_number) DO NOTHING;
```

**Lưu ý**: 
- Password đã được hash bằng BCrypt
- Nếu SĐT đã tồn tại, sẽ không tạo lại (ON CONFLICT DO NOTHING)

---

## Đổi mật khẩu Admin

Nếu muốn đổi mật khẩu, hash password mới bằng BCrypt và update:

```sql
-- Hash password mới (ví dụ: "newpassword")
-- Có thể dùng online tool: https://bcrypt-generator.com/
-- Hoặc dùng Java code để hash

UPDATE users 
SET password = '$2a$10$HASHED_PASSWORD_HERE' 
WHERE phone_number = '0123456789';
```

---

## Kiểm tra Admin đã tạo

```sql
SELECT id, fullname, phone_number, role, is_active 
FROM users 
WHERE role = 'ADMIN';
```

---

**Chúc bạn thành công! 🚀**


