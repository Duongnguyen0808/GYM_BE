-- Migration: Thêm hỗ trợ PT và TimeSlot cho gói PT
-- Thêm assigned_pt_id vào bảng packages
-- Thêm time_slot vào bảng member_packages

-- 1. Thêm cột assigned_pt_id vào bảng packages (cho gói PT)
ALTER TABLE packages ADD COLUMN IF NOT EXISTS assigned_pt_id BIGINT;

-- 2. Thêm foreign key constraint cho assigned_pt_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_packages_assigned_pt'
    ) THEN
        ALTER TABLE packages 
        ADD CONSTRAINT fk_packages_assigned_pt 
        FOREIGN KEY (assigned_pt_id) REFERENCES users(id);
    END IF;
END $$;

-- 3. Thêm cột time_slot vào bảng member_packages (cho gói PT)
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS time_slot VARCHAR(50);

-- 4. Tạo index để tối ưu truy vấn kiểm tra khung giờ đã được đặt
CREATE INDEX IF NOT EXISTS idx_member_packages_pt_timeslot_status 
ON member_packages(assigned_pt_id, time_slot, status) 
WHERE assigned_pt_id IS NOT NULL AND time_slot IS NOT NULL;

-- Verify
SELECT 'Migration V3 completed: Đã thêm hỗ trợ PT và TimeSlot' AS status;

