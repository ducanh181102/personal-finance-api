package com.example.personalfinance.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    @NotBlank(message = "Tên tài khoản không được trống.")
    @Size(max = 100, message = "Tên tài khoản tối đa 100 kí tự.")
    private final String name;
    @NotBlank(message = "Mật khẩu không được trống.")
    private final String password;
    @NotBlank(message = "Email không được trống.")
    @Size(max = 255, message = "Email tối đa 255 kí tự.")
    @Email(message = "Email không đúng định dạng.")
    private final String email;

    public CreateUserRequest(String name, String password, String email) {
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
