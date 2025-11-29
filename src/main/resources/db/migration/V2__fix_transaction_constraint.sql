-- Xóa unique constraint cũ trên member_package_id
-- Vì một gói tập có thể có nhiều transactions (mua, gia hạn, nâng cấp, hoàn tiền...)

-- Kiểm tra xem constraint có tồn tại không rồi mới drop
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk9d0cpbxgo47a0m2auiscge02l'
    ) THEN
        ALTER TABLE transactions DROP CONSTRAINT uk9d0cpbxgo47a0m2auiscge02l;
    END IF;
END $$;

-- Nếu có unique index, drop nó luôn
DROP INDEX IF EXISTS uk9d0cpbxgo47a0m2auiscge02l;




















