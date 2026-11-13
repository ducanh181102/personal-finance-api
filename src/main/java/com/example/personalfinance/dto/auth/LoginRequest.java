package com.example.personalfinance.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @Email(message = "Email không đúng định dạng.")
    @NotBlank(message = "Email không được trống.")
    private final String email;
    @NotBlank(message = "Mật khẩu không được trống.")
    private final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
