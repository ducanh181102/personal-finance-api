package com.example.personalfinance.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.NumberFormat;

import java.math.BigDecimal;

public class CreateAccountRequest {
    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(max = 100, message = "Tên tài khoản tối đa 100 kí tự")
    private String name;
    @Size(max = 30, message = "Loại tài khoản tối đa 30 kí tự")
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String type;
    @NotNull(message = "Số tiền ban đầu phải có trị")
    @Digits(integer = 17, fraction = 2, message = "Số dư tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
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
