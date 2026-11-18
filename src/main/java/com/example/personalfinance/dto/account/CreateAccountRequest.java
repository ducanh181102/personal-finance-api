package com.example.personalfinance.dto.account;

import com.example.personalfinance.model.AccountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateAccountRequest {
    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(max = 100, message = "Tên tài khoản tối đa 100 kí tự")
    private final String name;
    @NotNull(message = "Chọn loại ví.")
    private final AccountType type;
    @NotNull(message = "Số tiền ban đầu phải có trị")
    @Digits(integer = 17, fraction = 2, message = "Số dư tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
    private final BigDecimal openingBalance;

    public CreateAccountRequest(String name, AccountType type, BigDecimal openingBalance) {
        this.name = name;
        this.type = type;
        this.openingBalance = openingBalance;
    }

    public String getName() {
        return name;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }
}
