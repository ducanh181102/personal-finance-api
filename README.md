# Personal Finance API

Đồ án học Java và Spring Boot thông qua việc xây dựng REST API quản lý tài chính cá nhân. Phiên bản hiện tại tập trung vào quản lý tài khoản: tạo tài khoản, xem danh sách, tìm theo ID, đổi tên và xóa.

## Công nghệ

| Thành phần | Sử dụng |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Web MVC | Nhận yêu cầu HTTP và trả JSON |
| Spring Data JPA / Hibernate | Ánh xạ đối tượng Java và thao tác với database |
| H2 | Lưu database vào file trên máy |
| Jakarta Validation | Kiểm tra dữ liệu đầu vào |
| Maven Wrapper | Chạy, build và kiểm thử dự án |

## Chạy dự án

Cài JDK 21 và Git. Maven Wrapper có sẵn trong repository nên không cần cài Maven riêng. Lần chạy đầu cần kết nối mạng để tải Maven và các dependency.

```bash
git clone https://github.com/ducanh181102/personal-finance-api.git
cd personal-finance-api
./mvnw spring-boot:run
```

Trên Windows, dùng `mvnw.cmd spring-boot:run`. Bạn cũng có thể mở dự án trong IntelliJ IDEA, chọn JDK 21 và chạy hàm `main` trong `PersonalFinanceApiApplication`.

Địa chỉ mặc định: `http://localhost:8080`.

Kiểm tra ứng dụng:

```bash
curl http://localhost:8080/api/health
```

Kết quả: `Personal Finance API is running`.

## API tài khoản

| Method | Đường dẫn | Chức năng | Thành công |
| --- | --- | --- | --- |
| GET | `/api/accounts` | Xem danh sách tài khoản | 200 |
| GET | `/api/accounts/{id}` | Tìm tài khoản theo ID | 200 |
| POST | `/api/accounts` | Tạo tài khoản | 201 |
| PATCH | `/api/accounts/{id}` | Đổi tên tài khoản | 200 |
| DELETE | `/api/accounts/{id}` | Xóa tài khoản | 204, body rỗng |

Các yêu cầu POST và PATCH nhận JSON trong body với header `Content-Type: application/json`.

### Tạo tài khoản

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ví tiền mặt","type":"CASH","openingBalance":500000}'
```

Ví dụ phản hồi 201:

```json
{
  "id": 1,
  "name": "Ví tiền mặt",
  "type": "CASH",
  "openingBalance": 500000
}
```

ID do database sinh. Dùng ID thực tế từ phản hồi cho các thao tác tiếp theo; giá trị không nhất thiết bắt đầu từ 1 hoặc liên tiếp.

### Xem tài khoản

```bash
curl http://localhost:8080/api/accounts
curl http://localhost:8080/api/accounts/1
```

API danh sách trả về một mảng JSON, hoặc `[]` khi chưa có tài khoản.

### Đổi tên tài khoản

```bash
curl -X PATCH http://localhost:8080/api/accounts/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ví chi tiêu cá nhân"}'
```

Phản hồi 200 chứa tài khoản với tên mới, giữ nguyên ID, loại và số dư ban đầu.

### Xóa tài khoản

```bash
curl -i -X DELETE http://localhost:8080/api/accounts/1
```

Xóa thành công trả 204 và không có body. Tìm, đổi tên hoặc xóa một ID không tồn tại sẽ trả 404 nếu dữ liệu đầu vào hợp lệ.

## Validation và phản hồi lỗi

| Thao tác | Quy tắc kiểm tra trong DTO |
| --- | --- |
| Tạo tài khoản | `name` và `type` không được null, rỗng hoặc chỉ chứa khoảng trắng; `openingBalance` không được null |
| Đổi tên | `name` không được null, rỗng hoặc chỉ chứa khoảng trắng; tối đa 100 ký tự |

`type` hiện là chuỗi, ví dụ `CASH`, `BANK` hoặc `MOMO`; chưa giới hạn bằng enum. `openingBalance` sử dụng `BigDecimal` và hiện chưa có quy tắc bắt buộc số dư không âm.

Các cột database giới hạn tên ở 100 ký tự, loại ở 30 ký tự và số dư ở `DECIMAL(19,2)`. DTO tạo tài khoản hiện chưa kiểm tra đầy đủ các giới hạn này.

Lỗi validation trả 400 với tên trường và thông báo, ví dụ:

```json
{
  "name": "Tên tài khoản không được để trống"
}
```

JSON sai cú pháp hoặc không chuyển được sang kiểu dữ liệu cần thiết, chẳng hạn `"openingBalance": "abc"`, trả 400:

```json
{
  "message": "JSON không hợp lệ hoặc dữ liệu sai kiểu"
}
```

Không tìm thấy tài khoản trả 404:

```json
{
  "message": "Không tìm thấy tài khoản có ID: 99"
}
```

## Lưu trữ dữ liệu

Cấu hình hiện tại trong `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:file:./data/personalfinance;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

H2 lưu database trong thư mục `data/`, tính từ thư mục làm việc khi chạy ứng dụng. Chạy từ thư mục gốc dự án để luôn dùng cùng một database. Dữ liệu được giữ sau khi khởi động lại ứng dụng; thư mục `data/` được bỏ qua bởi Git.

## Cấu trúc source

```text
src/main/java/com/example/personalfinance/
├── controller/   # Nhận HTTP request và tạo phản hồi
├── dto/          # Dữ liệu đầu vào và quy tắc validation
├── exception/    # Exception riêng và xử lý lỗi chung
├── model/        # JPA Entity
├── repository/   # Truy cập database
└── service/      # Xử lý nghiệp vụ
```

Luồng chính: `Controller → Service → Repository → Database`. Các exception được hỗ trợ sẽ được `GlobalExceptionHandler` chuyển thành phản hồi HTTP tương ứng.

## Build và kiểm thử

Build file JAR, đồng thời dùng database trong bộ nhớ cho quá trình kiểm thử:

```bash
./mvnw clean package -Dspring.datasource.url=jdbc:h2:mem:personalfinance-test
```

Chạy file đã build:

```bash
java -jar target/personal-finance-api-0.0.1-SNAPSHOT.jar
```

Chỉ chạy kiểm thử:

```bash
./mvnw test -Dspring.datasource.url=jdbc:h2:mem:personalfinance-test
```

Tham số database trong các lệnh kiểm thử giúp tách dữ liệu test khỏi database file dùng khi phát triển. Hiện repository có kiểm thử khởi động Spring context; các ví dụ API ở trên có thể dùng để kiểm tra thủ công bằng curl hoặc Postman.

## Phạm vi hiện tại

Dự án đang phát triển và hiện cung cấp CRUD tài khoản cùng validation và xử lý lỗi. Các chức năng thu chi, danh mục giao dịch, báo cáo và xác thực người dùng chưa được triển khai.
