package com.example.personalfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MonthlyDashboardResponse {
    private final LocalDate month;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal netChange;
    private final int accountCount;
    private final int budgetCount;
    private final int overBudgetCount;

    public LocalDate getMonth() {
        return month;
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

    public int getAccountCount() {
        return accountCount;
    }

    public int getBudgetCount() {
        return budgetCount;
    }

    public int getOverBudgetCount() {
        return overBudgetCount;
    }

    public MonthlyDashboardResponse(LocalDate month, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netChange, int accountCount, int budgetCount, int overBudgetCount) {
        this.month = month;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netChange = netChange;
        this.accountCount = accountCount;
        this.budgetCount = budgetCount;
        this.overBudgetCount = overBudgetCount;
    }
}
