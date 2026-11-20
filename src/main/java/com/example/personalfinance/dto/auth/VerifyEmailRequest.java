package com.example.personalfinance.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "Liên kết không hợp lệ hoặc đã hết hạn.")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Liên kết không hợp lệ hoặc đã hết hạn.")
        String token
) {
}
