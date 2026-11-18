package com.example.personalfinance.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {
    @Size(max = 100, message = "Tên tài khoản tối đa 100 kí tự.")
    private final String name;
    private final String password;
    private final String currentPassword;
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 255, message = "Email tối đa 255 kí tự.")
    private final String email;

    public UpdateUserRequest(String name, String password, String email, String currentPassword) {
        this.name = name;
        this.password = password;
        this.email = email;
        this.currentPassword = currentPassword;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }
}
