package com.example.personalfinance.dto.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BudgetSummaryResponse {
    private final int budgetId;
    private final int categoryId;
    private final String categoryName;
    private final LocalDate month;
    private final BigDecimal limitAmount;
    private final BigDecimal spentAmount;
    private final BigDecimal remainingAmount;
    private final BigDecimal usagePercentage;
    private final boolean overBudget;

    public BigDecimal getUsagePercentage() {
        return usagePercentage;
    }

    public boolean isOverBudget() {
        return overBudget;
    }

    public BudgetSummaryResponse(int budgetId, int categoryId, String categoryName, LocalDate month, BigDecimal limitAmount, BigDecimal spentAmount, BigDecimal remainingAmount, BigDecimal usagePercentage, boolean overBudget) {
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.month = month;
        this.limitAmount = limitAmount;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
        this.usagePercentage = usagePercentage;
        this.overBudget = overBudget;
    }

    public int getBudgetId() {
        return budgetId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public LocalDate getMonth() {
        return month;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }
}
