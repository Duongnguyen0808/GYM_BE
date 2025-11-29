# 📖 HƯỚNG DẪN CHẠY SQL TRONG PGADMIN 4

## Cách chạy SQL trong pgAdmin 4

### Bước 1: Mở pgAdmin 4
- Khởi động pgAdmin 4 trên máy tính

### Bước 2: Kết nối database
1. Mở rộng **Servers** ở sidebar bên trái
2. Mở rộng server của bạn (thường là **PostgreSQL 15** hoặc tương tự)
3. Mở rộng **Databases**
4. Click chuột phải vào database **gym_management_db**
5. Chọn **Query Tool** (hoặc nhấn **Alt + Shift + Q**)

### Bước 3: Chạy SQL
1. Mở file SQL bạn muốn chạy:
   - `BE/clear_all_data.sql` - Xóa hết dữ liệu, chỉ giữ admin
   - `BE/create_admin.sql` - Tạo tài khoản admin
2. Copy toàn bộ nội dung file SQL
3. Paste vào Query Tool
4. Nhấn **F5** hoặc click nút **Execute/Refresh** (▶️) để chạy

### Bước 4: Kiểm tra kết quả
- Xem kết quả ở tab **Messages** (phía dưới)
- Nếu thành công sẽ hiện: "Query returned successfully"

---

## Các file SQL có sẵn

### 1. `clear_all_data.sql`
- **Mục đích**: Xóa hết dữ liệu, chỉ giữ lại admin
- **Khi nào dùng**: Khi muốn reset database về trạng thái ban đầu

### 2. `create_admin.sql`
- **Mục đích**: Tạo tài khoản admin
- **Khi nào dùng**: Khi cần tạo admin mới hoặc admin bị xóa

---

## Lưu ý

- ⚠️ **Backup database** trước khi chạy `clear_all_data.sql`
- ✅ Đảm bảo đã kết nối đúng database
- ✅ Kiểm tra kết quả sau khi chạy

---

**Chúc bạn thành công! 🚀**


