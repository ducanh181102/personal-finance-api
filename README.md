# Personal Finance API

Đồ án học Java và Spring Boot thông qua việc xây dựng REST API quản lý tài khoản, ghi nhận giao dịch thu chi và xem báo cáo theo tài khoản.

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

Xóa thành công trả `204` với body rỗng. Nếu tài khoản có giao dịch, hãy xóa các giao dịch đó trước vì chúng tham chiếu đến tài khoản. Tìm, đổi tên hoặc xóa một ID không tồn tại sẽ trả `404` nếu dữ liệu đầu vào hợp lệ.

## API giao dịch

| Method | Đường dẫn | Chức năng | Thành công |
| --- | --- | --- | --- |
| POST | `/api/transactions` | Tạo giao dịch thu hoặc chi | 201 |
| GET | `/api/transactions` | Xem danh sách, có thể lọc theo tài khoản và ngày | 200 |
| GET | `/api/transactions/{id}` | Xem một giao dịch theo ID | 200 |
| DELETE | `/api/transactions/{id}` | Xóa giao dịch | 204, body rỗng |

Tạo một giao dịch chi bằng ID tài khoản trả về ở bước tạo tài khoản.

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{"accountId":1,"type":"EXPENSE","amount":45000,"note":"Ăn trưa"}'
```

Dùng `INCOME` khi ghi nhận khoản thu. Máy chủ tự gán `transactionDate` theo thời điểm tạo giao dịch. Phản hồi chứa `id` của giao dịch và thông tin `account` liên kết.

```bash
curl http://localhost:8080/api/transactions
curl 'http://localhost:8080/api/transactions?accountId=1'
curl 'http://localhost:8080/api/transactions?accountId=1&from=2026-11-01&to=2026-11-07'
curl http://localhost:8080/api/transactions/1
curl -i -X DELETE http://localhost:8080/api/transactions/1
```

Khi lọc theo ngày, gửi đủ `accountId`, `from` và `to`. Hai ngày dùng định dạng `YYYY-MM-DD` và đều được tính trong kết quả. Đổi ngày ví dụ theo `transactionDate` của dữ liệu bạn muốn xem. Danh sách rỗng trả `[]`.

## Số dư và báo cáo

| Method | Đường dẫn | Dữ liệu trả về |
| --- | --- | --- |
| GET | `/api/accounts/{id}/balance` | ID tài khoản và `currentBalance` |
| GET | `/api/accounts/{id}/summary` | ID tài khoản, tổng thu, tổng chi và số dư hiện tại |
| GET | `/api/accounts/{id}/period-summary?from=YYYY-MM-DD&to=YYYY-MM-DD` | ID tài khoản, khoảng ngày, tổng thu, tổng chi và chênh lệch trong kỳ |

Cả ba endpoint trả `200 OK` khi thành công. Số dư hiện tại được tính theo công thức `currentBalance = openingBalance + totalIncome - totalExpense` trên toàn bộ giao dịch của tài khoản.

```bash
curl 'http://localhost:8080/api/accounts/1/period-summary?from=2026-11-01&to=2026-11-07'
```

Ví dụ với một khoản thu `100000` và một khoản chi `45000` trong kỳ.

```json
{
  "accountId": 1,
  "from": "2026-11-01",
  "to": "2026-11-07",
  "totalIncome": 100000,
  "totalExpense": 45000,
  "netChange": 55000
}
```

`netChange = totalIncome - totalExpense` và chỉ tính giao dịch trong khoảng ngày yêu cầu. Nếu khoảng ngày hợp lệ không có giao dịch, cả ba trường tiền đều bằng `0`.

## Validation và phản hồi lỗi

| Thao tác | Quy tắc kiểm tra trong DTO |
| --- | --- |
| Tạo tài khoản | `name` và `type` bắt buộc, tối đa lần lượt 100 và 30 ký tự. `openingBalance` bắt buộc, tối đa 17 chữ số phần nguyên và 2 chữ số thập phân |
| Đổi tên | `name` phải có ít nhất một ký tự khác khoảng trắng và dài tối đa 100 ký tự |
| Tạo giao dịch | `accountId` và `amount` phải lớn hơn 0. `amount` tối đa 17 chữ số phần nguyên và 2 chữ số thập phân. `type` là `INCOME` hoặc `EXPENSE`. `note` tùy chọn, tối đa 255 ký tự |

Loại tài khoản dùng chuỗi như `CASH`, `BANK` hoặc `MOMO`. Số dư ban đầu có thể âm.

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

Tài khoản hoặc giao dịch không tồn tại trả `404`.

```json
{
  "message": "Không tìm thấy đối tượng có ID: 99"
}
```

Ngày sai định dạng, thiếu tham số ngày bắt buộc hoặc `from` sau `to` trả `400`. Lọc giao dịch theo ngày mà thiếu `accountId` cũng trả `400`.

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
├── dto/          # Dữ liệu đầu vào, validation và dữ liệu phản hồi
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

Tham số database trong các lệnh kiểm thử giúp tách dữ liệu test khỏi database file dùng khi phát triển. Bộ test kiểm tra Spring khởi động, báo cáo theo ngày tính đúng số tiền và API trả `400` khi khoảng ngày bị đảo. Có thể chạy riêng các ca báo cáo với `./mvnw -Dtest=AccountPeriodSummaryTest test`.
