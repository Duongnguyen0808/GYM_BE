# BACKEND - GYM MANAGEMENT SERVICE

Spring Boot Backend cho hệ thống quản lý phòng tập gym.

## 🚀 Quick Start

```bash
# 1. Đảm bảo PostgreSQL đã chạy và tạo database
# Tên database: gym_management_db
# Username: postgres
# Password: 1 (hoặc sửa trong application.properties)

# 2. Build và chạy
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run

# 3. Backend sẽ chạy tại: http://localhost:8080
```

## 📋 Yêu cầu

- Java 17+
- Maven 3.6+ (hoặc dùng mvnw có sẵn)
- PostgreSQL 12+

## ⚙️ Cấu hình

### Database

Sửa file `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gym_management_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### Server

```properties
server.port=8080
server.address=0.0.0.0  # Cho phép truy cập từ mạng local
```

## 🔧 API Endpoints

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Base**: http://localhost:8080/api

### Các API chính:

- `/api/auth/**` - Authentication
- `/api/packages/**` - Quản lý gói tập
- `/api/members/**` - Quản lý hội viên
- `/api/subscriptions/**` - Quản lý đăng ký
- `/api/check-in/**` - Check-in/Check-out
- `/api/products/**` - Quản lý sản phẩm
- `/api/sales/**` - Bán hàng
- `/api/analytics/**` - Thống kê

## 🔐 Security

- JWT Authentication cho API
- Session-based cho Web interface
- CORS đã được cấu hình cho mobile app

## 📦 Build

```bash
# Build JAR file
./mvnw clean package

# Chạy JAR
java -jar target/gym-management-service-0.0.1-SNAPSHOT.jar
```

## 🗄️ Database

- Schema tự động tạo khi chạy lần đầu (ddl-auto=update)
- Có thể chạy script SQL: `insert_sample_data.sql`

## 📝 Notes

- Backend bind to `0.0.0.0` để mobile app có thể kết nối
- CORS cho phép tất cả origins (development mode)
- JWT token expiration: 24 giờ


