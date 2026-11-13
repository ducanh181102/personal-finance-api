package com.example.personalfinance.dto.budget;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateBudgetRequest {
    @NotNull(message = "ID danh mục không được để trống")
    @Positive(message = "ID danh mục phải lớn hơn 0")
    private final Integer categoryId;
    @NotNull(message = "Tháng không được trống")
    private final LocalDate month;
    @NotNull(message = "Ngân sách không được trống")
    @Positive(message = "Ngân sách phải là số dương")
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Ngân sách tối đa 17 chữ số phần nguyên và 2 chữ số thập phân"
    )
    private final BigDecimal limitAmount;

    public Integer getCategoryId() {
        return categoryId;
    }

    public LocalDate getMonth() {
        return month;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public CreateBudgetRequest(Integer categoryId, LocalDate month, BigDecimal limitAmount) {
        this.categoryId = categoryId;
        this.month = month;
        this.limitAmount = limitAmount;
    }
}
