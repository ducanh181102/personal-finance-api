package com.example.personalfinance.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email không được trống.")
        @Email(message = "Email không đúng định dạng.")
        @Size(max = 255, message = "Email tối đa 255 kí tự.")
        String email
) {
}
