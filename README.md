# Personal Finance API

REST API quản lý tài chính cá nhân: tài khoản, giao dịch thu chi, ngân sách, giao dịch định kỳ, chuyển tiền nội bộ, mục tiêu tiết kiệm và công nợ. Đây là đồ án học Java, Spring Boot, JPA/Hibernate và REST API.

## Công nghệ

| Thành phần | Sử dụng |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Web MVC | HTTP API và JSON |
| Spring Data JPA / Hibernate | Ánh xạ entity và truy cập database |
| H2 | Database file cục bộ |
| Jakarta Validation | Validation dữ liệu đầu vào |
| Spring Security | Bảo vệ endpoint và phân quyền theo user sở hữu dữ liệu |
| JJWT | Tạo, kiểm tra JWT Bearer token |
| Maven Wrapper | Build và chạy kiểm thử |

## Chạy dự án

Cần JDK 21. Maven Wrapper đã có sẵn nên không cần cài Maven riêng.

```bash
git clone https://github.com/ducanh181102/personal-finance-api.git
cd personal-finance-api
./mvnw spring-boot:run
```

Trên Windows dùng `mvnw.cmd spring-boot:run`. Ứng dụng mặc định chạy tại `http://localhost:8080`.

## Xác thực và phân quyền

Đăng ký, đăng nhập và khôi phục tài khoản là endpoint public. Tất cả endpoint dữ liệu khác cần JWT trong header:

```http
Authorization: Bearer <accessToken>
```

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| POST | `/api/users` | Đăng ký user | 201 |
| POST | `/api/auth/login` | Đăng nhập, nhận JWT | 200 |
| POST | `/api/auth/reactivate` | Khôi phục user đã vô hiệu hóa | 200 |
| GET | `/api/users/me` | Xem user hiện tại | 200 |
| PATCH | `/api/users/me` | Cập nhật user hiện tại | 200 |
| DELETE | `/api/users/me` | Vô hiệu hóa user hiện tại | 204 |

Ví dụ đăng ký:

```json
{
  "name": "Đức Anh",
  "email": "anh@example.com",
  "password": "mat-khau-cua-ban"
}
```

Ví dụ đăng nhập:

```json
{
  "email": "anh@example.com",
  "password": "mat-khau-cua-ban"
}
```

Response login chứa `accessToken` và thông tin user, không bao giờ trả về `passwordHash`.

User chỉ có thể xem, tạo, sửa hoặc xóa dữ liệu thuộc chính họ: Account, Category, Transaction, Budget, Transfer, Savings Goal, Goal Contribution, Debt và Debt Payment. Không có token, token sai/hết hạn trả `401`; có token nhưng truy cập dữ liệu của user khác trả `403`.

`DELETE /api/users/me` là **soft delete**: đặt `active = false`, không xóa lịch sử tài chính. Token cũ bị từ chối; user có thể dùng `/api/auth/reactivate` với email và mật khẩu cũ để nhận token mới.

## Luồng dữ liệu

```text
User ──< Account ──< Transaction >── Category
  │       │                           │
  │       ├──< RecurringTransaction >─┘
  │       ├──< Transfer (gửi / nhận) >── Account
  │       ├──< GoalContribution >── SavingsGoal
  │       └──< DebtPayment >── Debt
  │
  ├──< SavingsGoal
  └──< Debt

Category ──< Budget (một category, một tháng)
```

- `TransactionType` chỉ có `INCOME` và `EXPENSE`.
- Transaction phải có category cùng type với transaction.
- Chỉ category `EXPENSE` mới tạo được budget.
- Budget không lưu số tiền đã chi; số tiền này luôn được tính từ các transaction.
- `RecurringTransaction` là mẫu giao dịch lặp lại; nó sinh ra `Transaction` thật khi đến hạn.
- `Transfer` di chuyển tiền giữa hai Account cùng thuộc một user.
- `SavingsGoal` theo dõi một mục tiêu tiết kiệm; `GoalContribution` là từng lần góp vào mục tiêu.
- `Debt` theo dõi khoản cho mượn hoặc đi vay; `DebtPayment` là từng lần thanh toán.

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

### Transfer

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/transfers` | Danh sách giao dịch chuyển tiền | 200 |
| GET | `/api/transfers/{id}` | Xem một giao dịch chuyển tiền | 200 |
| POST | `/api/transfers` | Chuyển tiền giữa hai account | 201 |
| DELETE | `/api/transfers/{id}` | Xóa bản ghi chuyển tiền nhập sai | 204 |

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 200000,
  "transferDate": "2026-11-10T10:00:00",
  "note": "Chuyển sang ví ngân hàng"
}
```

Hai account phải cùng thuộc user hiện tại và không được trùng nhau. Transfer không có `PATCH`; nếu nhập sai thì xóa và tạo lại. Khi tính số dư, account gửi bị trừ và account nhận được cộng số tiền transfer.

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

Response phân trang có cấu trúc ổn định:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### Recurring transaction

Recurring transaction là mẫu cho các khoản lặp lại như lương, tiền trọ hoặc tiền điện. Mẫu không được tính vào dashboard cho đến khi scheduler tạo ra `Transaction` thật.

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/recurring-transactions` | Danh sách mẫu giao dịch định kỳ của user hiện tại | 200 |
| GET | `/api/recurring-transactions/{id}` | Xem một mẫu | 200 |
| POST | `/api/recurring-transactions` | Tạo mẫu | 201 |
| PATCH | `/api/recurring-transactions/{id}` | Cập nhật mẫu hoặc tạm dừng/kích hoạt lại | 200 |
| DELETE | `/api/recurring-transactions/{id}` | Xóa mẫu, không xóa lịch sử giao dịch đã sinh | 204 |

Ví dụ tạo khoản tiền trọ mỗi tháng:

```json
{
  "accountId": 1,
  "categoryId": 2,
  "amount": 3000000,
  "note": "Tiền trọ hằng tháng",
  "frequency": "MONTHLY",
  "nextRunDate": "2026-12-05"
}
```

`active` dùng để tạm dừng hoặc kích hoạt lại mẫu. Khi cập nhật bằng `PATCH`, không gửi field nào thì field đó được giữ nguyên.

Scheduler tự chạy khi ứng dụng khởi động và mỗi ngày lúc `00:05` theo múi giờ `Asia/Ho_Chi_Minh`. Nó tìm các mẫu `active` có `nextRunDate` đã đến hạn, tạo `Transaction` thật với ngày giao dịch bằng `nextRunDate`, sau đó dời lịch sang tháng kế tiếp. Nếu server từng tắt nhiều tháng, scheduler tạo bù toàn bộ kỳ bị lỡ trong một lần chạy.

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

Số dư hiện tại của account được tính theo công thức:

```text
openingBalance + income - expense + transferIn - transferOut
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

### Savings Goal

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/savings-goals` | Danh sách mục tiêu tiết kiệm | 200 |
| GET | `/api/savings-goals/{id}` | Xem một mục tiêu | 200 |
| POST | `/api/savings-goals` | Tạo mục tiêu | 201 |
| PATCH | `/api/savings-goals/{id}` | Đổi tên, số tiền mục tiêu hoặc hạn hoàn thành | 200 |
| DELETE | `/api/savings-goals/{id}` | Xóa mục tiêu chưa có khoản góp | 204 |

```json
{
  "name": "Mua laptop",
  "targetAmount": 25000000,
  "deadline": "2027-06-01"
}
```

`currentAmount` và `completed` do hệ thống quản lý, client không được sửa trực tiếp. Khi `currentAmount >= targetAmount`, mục tiêu chuyển sang `completed = true`.

### Goal Contribution

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/goal-contributions` | Danh sách các lần góp | 200 |
| GET | `/api/goal-contributions/{id}` | Xem một lần góp | 200 |
| POST | `/api/goal-contributions` | Góp tiền vào mục tiêu | 201 |
| DELETE | `/api/goal-contributions/{id}` | Xóa lần góp nhập sai | 204 |

```json
{
  "savingsGoalId": 1,
  "accountId": 1,
  "amount": 500000,
  "contributedAt": "2026-11-10T10:00:00",
  "note": "Góp tháng 11"
}
```

Tạo contribution làm tăng `currentAmount`; xóa contribution làm giảm lại giá trị này. Mục tiêu đã có contribution không thể xóa cho đến khi xóa hết các contribution liên quan.

### Debt

`DebtType` gồm `LENT` (người khác nợ mình) và `BORROWED` (mình nợ người khác). `DebtStatus` gồm `OPEN` và `PAID`.

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/debts` | Danh sách khoản nợ | 200 |
| GET | `/api/debts/{id}` | Xem một khoản nợ | 200 |
| POST | `/api/debts` | Tạo khoản nợ | 201 |
| PATCH | `/api/debts/{id}` | Đổi người liên quan, hạn thanh toán hoặc ghi chú | 200 |
| DELETE | `/api/debts/{id}` | Xóa debt chưa có payment | 204 |

```json
{
  "personName": "Nam",
  "type": "LENT",
  "originalAmount": 2000000,
  "dueDate": "2026-12-31",
  "note": "Cho Nam mượn"
}
```

Khi tạo debt: `remainingAmount = originalAmount`, `status = OPEN`. `originalAmount` và `type` không được sửa trực tiếp sau khi tạo.

### Debt Payment

| Method | Đường dẫn | Chức năng | Status thành công |
| --- | --- | --- | --- |
| GET | `/api/debt-payments` | Danh sách lần thanh toán nợ | 200 |
| GET | `/api/debt-payments/{id}` | Xem một lần thanh toán | 200 |
| POST | `/api/debt-payments` | Tạo lần thanh toán | 201 |
| DELETE | `/api/debt-payments/{id}` | Xóa lần thanh toán nhập sai | 204 |

```json
{
  "debtId": 1,
  "accountId": 1,
  "amount": 500000,
  "paidAt": "2026-11-10T10:00:00",
  "note": "Trả lần đầu"
}
```

Mỗi payment làm giảm `remainingAmount`; không thể trả vượt số nợ còn lại. Khi remaining amount bằng `0`, debt chuyển `PAID`. Xóa payment sẽ cộng số tiền trở lại và đưa debt về `OPEN` nếu còn nợ.

Ở phase hiện tại, Debt/Debt Payment dùng để theo dõi nghĩa vụ nợ và **chưa tự tạo Transaction hoặc làm thay đổi số dư Account**.

## Validation và lỗi

- Các ID phải lớn hơn `0`.
- Amount và limit amount phải dương, tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.
- Category của transaction phải có type trùng với transaction.
- Không thể tạo budget cho category `INCOME`.
- `from` không được sau `to`.
- Không thể xóa Account hoặc Category đã có dữ liệu đang tham chiếu đến chúng.
- Không thể chuyển tiền sang account của user khác, góp tiền vào goal của user khác, hoặc thanh toán debt của user khác.
- Không thể trả debt vượt `remainingAmount`.

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
- JWT bắt buộc cho API protected, owner truy cập được dữ liệu của mình và user khác nhận `403`.
- List/pagination không làm lộ Transaction hoặc Budget của user khác.
- Không thể xóa Account đã có Transaction; Account trống xóa thành công.
- Soft delete từ chối token cũ; khôi phục user trả token mới dùng được.
- Recurring scheduler tạo bù đủ transaction bị lỡ, không xử lý mẫu đã tạm dừng và dời đúng `nextRunDate`.
- Transfer làm số dư account gửi giảm, account nhận tăng; user khác không được dùng account không thuộc họ.
- Savings Goal cập nhật số tiền góp và trạng thái hoàn thành; không thể xóa goal đã có contribution.
- Debt Payment cập nhật số nợ còn lại, đóng/mở debt đúng lúc, chặn trả vượt nợ và chặn user khác truy cập.

## Cấu trúc source

```text
src/main/java/com/example/personalfinance/
├── controller/   # HTTP request/response
├── config/       # PasswordEncoder và Spring Security configuration
├── dto/
│   ├── account/      # DTO của account và báo cáo account
│   ├── auth/         # Đăng nhập và khôi phục tài khoản
│   ├── budget/       # DTO budget và budget summary
│   ├── category/     # DTO category và category summary
│   ├── common/       # DTO dùng chung, ví dụ PageResponse
│   ├── dashboard/    # DTO tổng quan dashboard
│   ├── debt/         # DTO khoản nợ
│   ├── debtpayment/  # DTO thanh toán nợ
│   ├── goalcontribution/ # DTO góp mục tiêu tiết kiệm
│   ├── recurring/    # DTO giao dịch định kỳ
│   ├── savingsgoal/  # DTO mục tiêu tiết kiệm
│   ├── transaction/  # DTO giao dịch
│   ├── transfer/     # DTO chuyển tiền
│   └── user/         # DTO user
├── exception/    # Xử lý lỗi chung
├── mapper/       # Chuyển entity thành response DTO
├── model/        # JPA entities
├── repository/   # Spring Data JPA
├── scheduler/    # Tác vụ tự chạy theo thời gian
├── security/     # JWT service và authentication filter
└── service/      # Nghiệp vụ
```

Luồng xử lý chính: `Controller → Service → Repository → Database`.
