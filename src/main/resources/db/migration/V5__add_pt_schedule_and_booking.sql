-- Migration: Thêm bảng pt_schedules và pt_bookings để quản lý lịch PT và đặt lịch

-- Bảng quản lý lịch available/unavailable của PT
CREATE TABLE IF NOT EXISTS pt_schedules (
    id BIGSERIAL PRIMARY KEY,
    pt_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT true,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pt_schedule_user FOREIGN KEY (pt_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_pt_schedule_date_slot UNIQUE (pt_id, schedule_date, time_slot)
);

-- Bảng quản lý booking của học viên với PT
CREATE TABLE IF NOT EXISTS pt_bookings (
    id BIGSERIAL PRIMARY KEY,
    member_package_id BIGINT NOT NULL,
    pt_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    cancellation_reason TEXT,
    cancelled_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pt_booking_member_package FOREIGN KEY (member_package_id) REFERENCES member_packages(id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_booking_user FOREIGN KEY (pt_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Tạo index để tối ưu truy vấn
CREATE INDEX IF NOT EXISTS idx_pt_schedules_pt_date ON pt_schedules(pt_id, schedule_date);
CREATE INDEX IF NOT EXISTS idx_pt_schedules_available ON pt_schedules(pt_id, schedule_date, is_available) WHERE is_available = true;
CREATE INDEX IF NOT EXISTS idx_pt_bookings_pt_date ON pt_bookings(pt_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_pt_bookings_member_package ON pt_bookings(member_package_id);
CREATE INDEX IF NOT EXISTS idx_pt_bookings_status ON pt_bookings(status);

SELECT 'Migration V5 completed: Đã thêm bảng pt_schedules và pt_bookings' AS status;






