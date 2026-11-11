package com.example.personalfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateAccountNameRequest {
    @NotBlank
    @Size(max = 100, message = "Tên tài khoản tối đa 100 kí tự")
    private final String name;

    public UpdateAccountNameRequest(String name, String type) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
