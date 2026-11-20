package com.example.personalfinance.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestVerifyEmailRequest(
        @Email(message = "Email không đúng định dạng.")
        @NotBlank(message = "Email không được trống.") String email
) {
}
