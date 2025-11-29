-- Migration: Thêm cột duration_months cho gói PT
-- Cho phép gói PT có thời hạn tính theo tháng (1, 2, 3 tháng...)

-- Thêm cột duration_months vào bảng packages
ALTER TABLE packages ADD COLUMN IF NOT EXISTS duration_months INTEGER;

-- Comment cho cột
COMMENT ON COLUMN packages.duration_months IS 'Thời hạn của gói PT tính theo tháng (chỉ dùng cho PT_SESSION). Ví dụ: 1 = 1 tháng, 2 = 2 tháng';

-- Verify
SELECT 'Migration V4 completed: Đã thêm duration_months cho gói PT' AS status;

