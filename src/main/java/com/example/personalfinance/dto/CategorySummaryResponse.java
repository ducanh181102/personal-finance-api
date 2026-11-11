package com.example.personalfinance.dto;

import com.example.personalfinance.model.TransactionType;

import java.math.BigDecimal;

public class CategorySummaryResponse {
    private final Integer categoryId;
    private final String categoryName;
    private final TransactionType type;
    private final BigDecimal totalAmount;

    public CategorySummaryResponse(Integer categoryId, String categoryName, TransactionType type, BigDecimal totalAmount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.type = type;
        this.totalAmount = totalAmount;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
