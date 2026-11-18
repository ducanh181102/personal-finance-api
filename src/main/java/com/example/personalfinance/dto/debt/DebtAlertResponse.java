package com.example.personalfinance.dto.debt;

import com.example.personalfinance.model.DebtType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DebtAlertResponse {
    private final int debtId;
    private final String personName;
    private final DebtType type;
    private final BigDecimal remainingAmount;
    private final BigDecimal scheduledAmount;
    private final LocalDate dueDate;
    private final boolean installment;
    private final long daysUntilDue;
    private final boolean overdue;

    public DebtAlertResponse(
            int debtId,
            String personName,
            DebtType type,
            BigDecimal remainingAmount,
            BigDecimal scheduledAmount,
            LocalDate dueDate,
            long daysUntilDue,
            boolean overdue,
            boolean installment
    ) {
        this.debtId = debtId;
        this.personName = personName;
        this.type = type;
        this.remainingAmount = remainingAmount;
        this.scheduledAmount = scheduledAmount;
        this.dueDate = dueDate;
        this.daysUntilDue = daysUntilDue;
        this.overdue = overdue;
        this.installment = installment;
    }

    // getter cho tất cả field

    public int getDebtId() {
        return debtId;
    }

    public String getPersonName() {
        return personName;
    }

    public DebtType getType() {
        return type;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public BigDecimal getScheduledAmount() {
        return scheduledAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getDaysUntilDue() {
        return daysUntilDue;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public boolean isInstallment() {
        return installment;
    }
}
