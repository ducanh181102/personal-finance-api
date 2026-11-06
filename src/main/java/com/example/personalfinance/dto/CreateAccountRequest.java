package com.example.personalfinance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.NumberFormat;

import java.math.BigDecimal;

public class CreateAccountRequest {
    @NotBlank(message = "Tên tài khoản không được để trống")
    private String name;
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String type;
    @NotNull(message = "Số tiền ban đầu phải có trị")
    private BigDecimal openingBalance;

    public CreateAccountRequest() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }
}
