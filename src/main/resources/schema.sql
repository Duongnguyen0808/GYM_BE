ALTER TABLE transactions ADD COLUMN IF NOT EXISTS kind VARCHAR(32);

UPDATE transactions t
SET kind = CASE
    WHEN t.sale_id IS NOT NULL THEN 'SALE'
    WHEN t.member_package_id IS NOT NULL THEN 'SUBSCRIPTION_NEW'
    ELSE 'SUBSCRIPTION_NEW'
END
WHERE t.kind IS NULL;
-- Thêm lý do hủy và lịch tập theo tuần cho member_packages
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS allowed_weekdays TEXT; -- Ví dụ: MON,WED,FRI
