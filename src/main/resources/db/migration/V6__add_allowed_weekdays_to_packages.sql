-- Migration: Thêm field allowedWeekdays vào bảng packages để lưu các thứ trong tuần cho gói PT

ALTER TABLE packages 
ADD COLUMN IF NOT EXISTS allowed_weekdays VARCHAR(100);

COMMENT ON COLUMN packages.allowed_weekdays IS 'Các thứ trong tuần cho phép tập, định dạng CSV: MON,WED,FRI (chỉ dùng cho PT_SESSION)';

SELECT 'Migration V6 completed: Đã thêm field allowed_weekdays vào bảng packages' AS status;






