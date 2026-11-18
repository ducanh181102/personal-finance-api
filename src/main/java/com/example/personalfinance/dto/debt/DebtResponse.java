package com.example.personalfinance.dto.debt;

import com.example.personalfinance.model.DebtStatus;
import com.example.personalfinance.model.DebtType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DebtResponse {
    private final int id;
    private final String personName;
    private final DebtType type;
    private final BigDecimal originalAmount;
    private final BigDecimal remainingAmount;
    private final LocalDate dueDate;
    private final BigDecimal monthlyInstallmentAmount;
    private final LocalDate firstInstallmentDate;
    private final LocalDate nextInstallmentDate;
    private final BigDecimal nextInstallmentAmount;
    private final long installmentCount;
    private final DebtStatus status;
    private final String note;

    public DebtResponse(int id,
                        String personName,
                        DebtType type,
                        BigDecimal originalAmount,
                        BigDecimal remainingAmount,
                        LocalDate dueDate,
                        BigDecimal monthlyInstallmentAmount,
                        LocalDate firstInstallmentDate,
                        LocalDate nextInstallmentDate,
                        BigDecimal nextInstallmentAmount,
                        long installmentCount,
                        DebtStatus status,
                        String note) {
        this.id = id;
        this.personName = personName;
        this.type = type;
        this.originalAmount = originalAmount;
        this.remainingAmount = remainingAmount;
        this.dueDate = dueDate;
        this.monthlyInstallmentAmount = monthlyInstallmentAmount;
        this.firstInstallmentDate = firstInstallmentDate;
        this.nextInstallmentDate = nextInstallmentDate;
        this.nextInstallmentAmount = nextInstallmentAmount;
        this.installmentCount = installmentCount;
        this.status = status;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public String getPersonName() {
        return personName;
    }

    public DebtType getType() {
        return type;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getMonthlyInstallmentAmount() {
        return monthlyInstallmentAmount;
    }

    public LocalDate getFirstInstallmentDate() {
        return firstInstallmentDate;
    }

    public LocalDate getNextInstallmentDate() {
        return nextInstallmentDate;
    }

    public BigDecimal getNextInstallmentAmount() {
        return nextInstallmentAmount;
    }

    public long getInstallmentCount() {
        return installmentCount;
    }

    public DebtStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
