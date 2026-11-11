# Postman — Personal Finance API

## Sử dụng

1. Khởi động ứng dụng Spring Boot trên cổng 8080.
2. Import `personal-finance-api.postman_collection.json` vào Postman.
3. Có thể import thêm `local.postman_environment.json` rồi chọn **Personal Finance - Local**. Collection đã có `base_url` mặc định nên environment là tùy chọn.
4. Mở collection **Personal Finance API - Local Tests**.
5. Chọn **Run** để chạy 24 request theo thứ tự với **1 iteration**, hoặc mở từng request và bấm **Send**.
6. Xem kết quả trong **Test Results** hoặc màn hình kết quả của Collection Runner.

## Các nhóm request

| Nhóm | Nội dung |
| --- | --- |
| 01–10 | Kiểm tra ứng dụng; tạo, xem, đổi tên, xóa tài khoản; xác nhận tài khoản đã xóa trả 404 |
| 11–21 | Kiểm tra validation, số dư sai kiểu, JSON sai cú pháp và thiếu body |
| 22–24 | Tìm, đổi tên, xóa ID không tồn tại |

Request 02 tự lưu ID vừa tạo vào collection variable `account_id`. Khi gửi riêng request 03–10, hãy chạy request 02 trước và tuân theo thứ tự; sau khi đã xóa, chạy lại request 02 để tạo tài khoản test mới. Các request phụ thuộc được bỏ qua nếu chưa có ID.

Luồng CRUD sử dụng dữ liệu giả có tên bắt đầu bằng **PM Test**, và request 07 xóa chính tài khoản đó. Không cần dùng ID của tài khoản đang có trong database. Nếu dừng trước request 07, tài khoản test có thể còn lại; có thể gửi request 07 để dọn sau.

Mã HTTP mong đợi được ghi trong tên nhóm, mô tả và script test. HTTP 500 là lỗi, không được coi là một kết quả đạt. Các file này chỉ chuẩn bị request và assertion; cần chạy chúng với ứng dụng của bạn để xác nhận hành vi API.

Nếu PATCH trả 400 hoặc 500 ngay cả khi JSON hợp lệ, kiểm tra Console Spring Boot và cách Jackson tạo `UpdateAccountNameRequest` (constructor hoặc cấu hình ánh xạ). Bộ request không chỉnh sửa source của ứng dụng.
