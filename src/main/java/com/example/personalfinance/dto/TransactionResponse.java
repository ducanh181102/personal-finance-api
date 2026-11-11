package com.example.personalfinance.dto;

import com.example.personalfinance.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    private final int id;
    private final int accountId;
    private final int categoryId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime transactionDate;
    private final String note;

    public int getId() {
        return id;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public String getNote() {
        return note;
    }

    public TransactionResponse(int id, int accountId, int categoryId, TransactionType type, BigDecimal amount, LocalDateTime transactionDate, String note) {
        this.id = id;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.note = note;
    }
}
