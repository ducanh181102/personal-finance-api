# Personal Finance API

REST API quản lý tài khoản cá nhân, giao dịch thu chi, danh mục và ngân sách theo tháng. Đây là đồ án học Java, Spring Boot, JPA/Hibernate và REST API.

## Công nghệ

| Thành phần | Sử dụng |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Web MVC | HTTP API và JSON |
| Spring Data JPA / Hibernate | Ánh xạ entity và truy cập database |
| H2 | Database file cục bộ |
| Jakarta Validation | Validation dữ liệu đầu vào |
| Maven Wrapper | Build và chạy kiểm thử |

## Chạy dự án

Cần JDK 21. Maven Wrapper đã có sẵn nên không cần cài Maven riêng.

```bash
git clone https://github.com/ducanh181102/personal-finance-api.git
cd personal-finance-api
./mvnw spring-boot:run
```

Trên Windows dùng `mvnw.cmd spring-boot:run`. Ứng dụng mặc định chạy tại `http://localhost:8080`.

```bash
curl http://localhost:8080/api/health
```

## Luồng dữ liệu

```text
Account ──< Transaction >── Category
                              │
                              └──< Budget (một category, một tháng)
```

- `TransactionType` chỉ có `INCOME` và `EXPENSE`.
- Transaction phải có category cùng type với transaction.
- Chỉ category `EXPENSE` mới tạo được budget.
- Budget không lưu số tiền đã chi; số tiền này luôn được tính từ các transaction.

## API

Tất cả request có body JSON cần header `Content-Type: application/json`.

### Account

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/accounts` | Danh sách tài khoản | 200 |
| GET | `/api/accounts/{id}` | Xem một tài khoản | 200 |
| POST | `/api/accounts` | Tạo tài khoản | 201 |
| PATCH | `/api/accounts/{id}` | Đổi tên tài khoản | 200 |
| DELETE | `/api/accounts/{id}` | Xóa tài khoản | 204 |

```json
{
  "name": "Ví tiền mặt",
  "type": "CASH",
  "openingBalance": 500000
}
```

### Category

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/categories` | Danh sách danh mục | 200 |
| GET | `/api/categories/{id}` | Xem một danh mục | 200 |
| POST | `/api/categories` | Tạo danh mục | 201 |
| PATCH | `/api/categories/{id}` | Đổi tên hoặc type | 200 |
| DELETE | `/api/categories/{id}` | Xóa danh mục | 204 |

```json
{
  "name": "Ăn uống",
  "type": "EXPENSE"
}
```

Category đang được transaction sử dụng không thể bị xóa. Type của category đang được sử dụng cũng không thể đổi, vì sẽ làm dữ liệu transaction cũ bị lệch nghiệp vụ.

### Transaction

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/transactions` | Danh sách giao dịch, có thể lọc | 200 |
| GET | `/api/transactions/page` | Danh sách có phân trang | 200 |
| GET | `/api/transactions/{id}` | Xem một giao dịch | 200 |
| POST | `/api/transactions` | Tạo giao dịch | 201 |
| PATCH | `/api/transactions/{id}` | Sửa amount, type hoặc note | 200 |
| DELETE | `/api/transactions/{id}` | Xóa giao dịch | 204 |

Ví dụ tạo khoản chi:

```json
{
  "accountId": 1,
  "categoryId": 1,
  "type": "EXPENSE",
  "amount": 120000,
  "transactionDate": "2026-11-08T12:30:00",
  "note": "Ăn trưa"
}
```

`transactionDate` là bắt buộc, dùng định dạng `YYYY-MM-DDTHH:mm:ss` và không được ở tương lai.

Lọc giao dịch theo account và khoảng ngày:

```text
GET /api/transactions?accountId=1&from=2026-11-01&to=2026-11-30
GET /api/transactions/page?accountId=1&page=0&size=10
GET /api/transactions/page?accountId=1&from=2026-11-01&to=2026-11-30&page=0&size=10
```

Khi lọc ngày, phải gửi đầy đủ `accountId`, `from` và `to`. Cả ngày bắt đầu và kết thúc đều được tính. `page` bắt đầu từ `0`; `size` nằm trong khoảng `1` đến `100`.

### Báo cáo tài khoản

| Method | Đường dẫn | Dữ liệu trả về |
| --- | --- | --- |
| GET | `/api/accounts/{id}/balance` | Số dư hiện tại |
| GET | `/api/accounts/{id}/summary` | Tổng thu, tổng chi và số dư hiện tại |
| GET | `/api/accounts/{id}/period-summary?from=...&to=...` | Tổng thu, chi và chênh lệch trong kỳ |
| GET | `/api/accounts/{id}/category-summary?from=...&to=...` | Tổng tiền theo từng category trong kỳ |

```text
GET /api/accounts/1/category-summary?from=2026-11-01&to=2026-11-30
```

### Budget

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/budgets` | Danh sách budget | 200 |
| GET | `/api/budgets/{id}` | Xem một budget | 200 |
| POST | `/api/budgets` | Tạo budget | 201 |
| PATCH | `/api/budgets/{id}` | Chỉ đổi hạn mức | 200 |
| DELETE | `/api/budgets/{id}` | Xóa budget | 204 |
| GET | `/api/budgets/{id}/summary` | Tổng quan một budget | 200 |
| GET | `/api/budgets/summary?month=YYYY-MM-01` | Tổng quan mọi budget trong tháng | 200 |

```json
{
  "categoryId": 1,
  "month": "2026-11-01",
  "limitAmount": 3000000
}
```

`month` bắt buộc là ngày đầu tháng. Mỗi category chỉ có một budget trong một tháng.

Ví dụ response summary:

```json
{
  "budgetId": 1,
  "categoryId": 1,
  "categoryName": "Ăn uống",
  "month": "2026-11-01",
  "limitAmount": 100000,
  "spentAmount": 120000,
  "remainingAmount": -20000,
  "usagePercentage": 120.00,
  "overBudget": true
}
```

`spentAmount` là tổng giao dịch của category trong tháng. `remainingAmount = limitAmount - spentAmount`; `overBudget` là `true` khi số đã chi lớn hơn hạn mức.

## Validation và lỗi

- Các ID phải lớn hơn `0`.
- Amount và limit amount phải dương, tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.
- Category của transaction phải có type trùng với transaction.
- Không thể tạo budget cho category `INCOME`.
- `from` không được sau `to`.

Validation hoặc lỗi nghiệp vụ trả `400 Bad Request`, ví dụ:

```json
{
  "transactionDate": "Thời gian giao dịch không được ở tương lai"
}
```

Đối tượng không tồn tại trả `404 Not Found`:

```json
{
  "message": "Không tìm thấy đối tượng có ID: 99"
}
```

## Database

```properties
spring.datasource.url=jdbc:h2:file:./data/personalfinance;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=update
```

H2 lưu data tại `data/personalfinance.mv.db`. Nếu thay đổi entity theo cách không tương thích với dữ liệu cũ khi đang học, dừng ứng dụng rồi xóa file database để Hibernate tạo schema mới. Thao tác này xóa toàn bộ dữ liệu cục bộ.

## Kiểm thử

```bash
./mvnw clean test -Dspring.datasource.url=jdbc:h2:mem:personalfinance-test
```

Các test hiện kiểm tra:

- Spring context khởi động.
- Báo cáo theo kỳ tính đúng thu, chi và chênh lệch.
- Khoảng ngày không hợp lệ trả `400`.
- Budget summary tính đúng tổng chi, phần trăm sử dụng, trạng thái vượt hạn mức và không lấy giao dịch của tháng sau.

## Cấu trúc source

```text
src/main/java/com/example/personalfinance/
├── controller/   # HTTP request/response
├── dto/          # Request DTO, validation, response DTO
├── exception/    # Xử lý lỗi chung
├── model/        # JPA entities
├── repository/   # Spring Data JPA
└── service/      # Nghiệp vụ
```

Luồng xử lý chính: `Controller → Service → Repository → Database`.
