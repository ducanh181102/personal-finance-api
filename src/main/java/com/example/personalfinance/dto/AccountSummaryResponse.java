package com.example.personalfinance.dto;

import java.math.BigDecimal;

public class AccountSummaryResponse {
    private final Integer accountId;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal currentBalance;

    public Integer getAccountId() {
        return accountId;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public AccountSummaryResponse(Integer accountId, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal currentBalance) {
        this.accountId = accountId;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.currentBalance = currentBalance;
    }
}
