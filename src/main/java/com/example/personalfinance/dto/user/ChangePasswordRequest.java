package com.example.personalfinance.dto.user;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Nhập mật khẩu hiện tại.") String currentPassword,
        @NotBlank(message = "Nhập mật khẩu mới.") String newPassword
) {
}
