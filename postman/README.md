# Postman — Personal Finance API

## Bộ lọc giao dịch có JWT

1. Import `transaction-filters.postman_collection.json` và chọn môi trường **Personal Finance - Local**.
2. Điền `test_email` và `test_password` của user test trong môi trường local, rồi gửi **01 - Đăng nhập và lưu JWT**. Các request sau tự dùng token đã lưu.
3. Gửi các request **02–08** để kiểm tra bộ lọc và phân trang. Tất cả dùng `GET /api/transactions/page`.

Dữ liệu mẫu dùng ví `account_id=1`, có 4 giao dịch vào `sample_date=2026-11-13` và tổng cộng 200 giao dịch qua hai trang. Nếu dùng database khác, cập nhật biến của collection theo dữ liệu test tương ứng.

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
