package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "debt_payments")
public class DebtPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "debt_id", nullable = false)
    private Debt debt;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @Column(length = 255)
    private String note;

    @OneToOne
    @JoinColumn(nullable = false, name = "transaction_id")
    private Transaction transaction;

    @OneToOne
    @JoinColumn(name = "reversal_transaction_id", unique = true)
    private Transaction reversalTransaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DebtPaymentStatus status;

    @Column
    private LocalDateTime cancelledAt;

    protected DebtPayment() {
    }

    public DebtPayment(Debt debt,
                       Account account,
                       BigDecimal amount,
                       LocalDateTime paidAt,
                       String note,
                       Transaction transaction) {
        this.debt = debt;
        this.account = account;
        this.amount = amount;
        this.paidAt = paidAt;
        this.note = note;
        this.transaction = transaction;
        this.status = DebtPaymentStatus.COMPLETED;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Transaction getReversalTransaction() {
        return reversalTransaction;
    }

    public DebtPaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void cancelDebtPayment(Transaction reversalTransaction, LocalDateTime cancelledAt) {
        this.status = DebtPaymentStatus.CANCELLED;
        this.reversalTransaction = reversalTransaction;
        this.cancelledAt = cancelledAt;
    }

    public int getId() {
        return id;
    }

    public Debt getDebt() {
        return debt;
    }

    public Account getAccount() {
        return account;
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
