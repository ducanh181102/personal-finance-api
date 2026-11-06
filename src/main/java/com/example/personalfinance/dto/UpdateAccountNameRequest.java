package com.example.personalfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateAccountNameRequest {
    @NotBlank
    @Size(max=100, message = "Tên tài khoản tối đa 100 kí tự")
    private String name;

    public UpdateAccountNameRequest(String name, String type) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
