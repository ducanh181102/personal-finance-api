package com.example.personalfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountPeriodSummaryResponse {
    private final Integer accountId;
    private final LocalDate from;
    private final LocalDate to;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal netChange;

    public AccountPeriodSummaryResponse(Integer accountId, LocalDate from, LocalDate to, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netChange) {
        this.accountId = accountId;
        this.from = from;
        this.to = to;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netChange = netChange;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getNetChange() {
        return netChange;
    }
}
