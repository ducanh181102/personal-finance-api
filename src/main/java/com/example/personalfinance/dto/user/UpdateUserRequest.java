package com.example.personalfinance.dto.user;

import jakarta.validation.constraints.Size;

public class UpdateUserRequest {
    @Size(max = 100, message = "Tên tài khoản tối đa 100 kí tự.")
    private final String name;
    private final String password;
    @Size(max = 255, message = "Email tối đa 255 kí tự.")
    private final String email;

    public UpdateUserRequest(String name, String password, String email) {
        this.name = name;
        this.password = password;
        this.email = email;
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
}
