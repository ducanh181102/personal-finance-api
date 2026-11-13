package com.example.personalfinance.dto.recurring;

import com.example.personalfinance.model.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringTransactionResponse {
    private final int id;
    private final int accountId;
    private final int categoryId;
    private final BigDecimal amount;
    private final String note;
    private final RecurrenceFrequency frequency;
    private final LocalDate nextRunDate;
    private final boolean active;

    public int getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public RecurringTransactionResponse(int id, int accountId, int categoryId, BigDecimal amount, String note, RecurrenceFrequency frequency, LocalDate nextRunDate, boolean active) {
        this.id = id;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.note = note;
        this.frequency = frequency;
        this.nextRunDate = nextRunDate;
        this.active = active;
    }
}
