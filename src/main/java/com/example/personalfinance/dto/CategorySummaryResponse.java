package com.example.personalfinance.dto;

import com.example.personalfinance.model.TransactionType;

import java.math.BigDecimal;

public class CategorySummaryResponse {
    private Integer categoryId;
    private String categoryName;
    private TransactionType type;
    private BigDecimal totalAmount;

    public CategorySummaryResponse() {
    }

    public CategorySummaryResponse(Integer categoryId, String categoryName, TransactionType type, BigDecimal totalAmount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.type = type;
        this.totalAmount = totalAmount;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
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
