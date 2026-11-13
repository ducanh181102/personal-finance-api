package com.example.personalfinance.dto.goalcontribution;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GoalContributionResponse {
    private final int id;
    private final int savingsGoalId;
    private final int accountId;
    private final BigDecimal amount;
    private final LocalDateTime contributedAt;
    private final String note;

    public GoalContributionResponse(int id, int savingsGoalId, int accountId, BigDecimal amount, LocalDateTime contributedAt, String note) {
        this.id = id;
        this.savingsGoalId = savingsGoalId;
        this.accountId = accountId;
        this.amount = amount;
        this.contributedAt = contributedAt;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public int getSavingsGoalId() {
        return savingsGoalId;
    }

    public int getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getContributedAt() {
        return contributedAt;
    }

    public String getNote() {
        return note;
    }
}
