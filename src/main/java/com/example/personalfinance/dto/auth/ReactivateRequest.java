package com.example.personalfinance.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ReactivateRequest {
    @Email(message = "Email không đúng định dạng.")
    @NotBlank(message = "Email không được trống.")
    private final String email;
    @NotBlank(message = "Mật khẩu không được trống.")
    private final String oldPassword;

    public String getEmail() {
        return email;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public ReactivateRequest(String email, String oldPassword) {
        this.email = email;
        this.oldPassword = oldPassword;
    }
}
