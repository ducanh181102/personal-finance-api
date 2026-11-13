package com.example.personalfinance.dto.debtpayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DebtPaymentResponse {
    private final int id;
    private final int debtId;
    private final int accountId;
    private final BigDecimal amount;
    private final LocalDateTime paidAt;
    private final String note;

    public DebtPaymentResponse(int id,
                               int debtId,
                               int accountId,
                               BigDecimal amount,
                               LocalDateTime paidAt,
                               String note) {
        this.id = id;
        this.debtId = debtId;
        this.accountId = accountId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public int getDebtId() {
        return debtId;
    }

    public int getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getNote() {
        return note;
    }
}
