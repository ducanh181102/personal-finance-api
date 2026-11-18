package com.example.personalfinance.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Liên kết không hợp lệ hoặc đã hết hạn.")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Liên kết không hợp lệ hoặc đã hết hạn.")
        String token,
        @NotBlank(message = "Mật khẩu không được trống.")
        @Size(max = 72, message = "Mật khẩu tối đa 72 byte UTF-8.")
        String password
) {
}
