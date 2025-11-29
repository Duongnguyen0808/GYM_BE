## Mục tiêu
- Đổi tên loại gói `Truy cập Gym` sang nhãn ngắn gọn, dễ hiểu và thống nhất trên toàn giao diện.
- Giữ nguyên logic xử lý; chỉ thay đổi chuỗi hiển thị và thông báo cho người dùng.

## Tên đề xuất
- Dùng nhãn: **Gói thời hạn** (ý nghĩa: thẻ vào phòng gym theo số ngày, không giới hạn lượt vào trong thời hạn).
- Các loại khác giữ nguyên:
  - **Gói PT** → “Gói PT cá nhân” (tùy chọn, vẫn hiển thị “Gói PT” nếu bạn muốn ngắn gọn)
  - **Theo lượt** → “Gói theo lượt”

## Thay đổi cụ thể
1. Enum hiển thị loại gói
- Sửa `PackageType` để thay `displayName`:
  - `GYM_ACCESS` → "Gói thời hạn"
  - (tuỳ chọn) `PT_SESSION` → "Gói PT cá nhân"
  - `PER_VISIT` → "Gói theo lượt"
- Tác động:
  - Dropdown “Loại gói” trên trang tạo/sửa gói (`admin/package-form.html`) tự hiển thị nhãn mới vì đang lấy `type.displayName`.

2. Danh sách gói tập (Admin)
- Điều chỉnh badge hiển thị loại gói ở `admin/packages.html`:
  - Hiện "Gói thời hạn" thay vì "Truy cập phòng Gym".

3. Thông báo/ngữ cảnh nghiệp vụ
- Việt hoá các thông báo còn chữ "Gym Access" trong service:
  - `CheckInService`: thay thông báo thành "Gói thời hạn" và "Gói theo lượt".
  - (Nếu có) `PaymentService` và nơi khác chỉ đổi chuỗi mô tả hiển thị.

## Kiểm thử
- Mở “Quản lý Gói tập” → “Tạo Gói tập mới”:
  - Dropdown “Loại gói” hiển thị "Gói thời hạn", "Gói PT", "Gói theo lượt".
- Tạo một gói thời hạn và quay lại danh sách:
  - Badge loại gói hiển thị "Gói thời hạn".
- Thử Check-in với hội viên có gói thời hạn:
  - Thông báo thành công/không đủ điều kiện hiển thị nhãn tiếng Việt mới.

## Phạm vi ảnh hưởng
- Chỉ thay chuỗi hiển thị; không đổi giá trị enum gốc, nên API/DB/logic giữ nguyên.
- Không ảnh hưởng dữ liệu hiện có.

## Tiến hành
- Thực hiện các chỉnh sửa chuỗi như trên và kiểm thử nhanh trên giao diện Admin và luồng Check-in.

Bạn xác nhận dùng nhãn “Gói thời hạn” cho loại `Truy cập Gym`? Nếu ok, tôi sẽ áp dụng ngay và kiểm thử.