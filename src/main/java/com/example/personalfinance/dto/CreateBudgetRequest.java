package com.example.personalfinance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateBudgetRequest {
    @NotNull(message = "ID danh mục không được để trống")
    @Positive(message = "ID danh mục phải lớn hơn 0")
    private Integer categoryId;
    @NotNull(message = "Tháng không được trống")
    private LocalDate month;
    @NotNull(message = "Ngân sách không được trống")
    @Positive(message = "Ngân sách phải là số dương")
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Ngân sách tối đa 17 chữ số phần nguyên và 2 chữ số thập phân"
    )
    private BigDecimal limitAmount;

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public void setMonth(LocalDate month) {
        this.month = month;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

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
