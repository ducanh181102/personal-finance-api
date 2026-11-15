package com.example.personalfinance.dto.debtpayment;

import com.example.personalfinance.model.DebtPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DebtPaymentResponse {
    private final int id;
    private final int debtId;
    private final int accountId;
    private final int transactionId;
    private final Integer reversalTransactionId;
    private final BigDecimal amount;
    private final LocalDateTime paidAt;
    private final String note;
    private final DebtPaymentStatus status;
    private final LocalDateTime cancelledAt;

    public DebtPaymentResponse(int id,
                               int debtId,
                               int accountId,
                               int transactionId,
                               Integer reversalTransactionId,
                               BigDecimal amount,
                               LocalDateTime paidAt,
                               String note,
                               DebtPaymentStatus status,
                               LocalDateTime cancelledAt) {
        this.id = id;
        this.debtId = debtId;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.reversalTransactionId = reversalTransactionId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.note = note;
        this.status = status;
        this.cancelledAt = cancelledAt;
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

    public int getTransactionId() {
        return transactionId;
    }

    public Integer getReversalTransactionId() {
        return reversalTransactionId;
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

    public DebtPaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
