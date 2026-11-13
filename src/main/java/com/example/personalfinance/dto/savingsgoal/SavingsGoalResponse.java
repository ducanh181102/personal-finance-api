package com.example.personalfinance.dto.savingsgoal;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsGoalResponse {
    private final int id;
    private final int userId;
    private final String name;
    private final BigDecimal targetAmount;
    private final BigDecimal currentAmount;
    private final LocalDate deadline;
    private final boolean completed;

    public SavingsGoalResponse(int id, int userId, String name, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate deadline, boolean completed) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public boolean isCompleted() {
        return completed;
    }
}
