package com.example.personalfinance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class UpdateBudgetRequest {
    @NotNull(message = "Ngân sách không được trống")
    @Positive(message = "Ngân sách phải là số dương")
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Ngân sách tối đa 17 chữ số phần nguyên và 2 chữ số thập phân"
    )
    private final BigDecimal limitAmount;

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public UpdateBudgetRequest(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }
}
