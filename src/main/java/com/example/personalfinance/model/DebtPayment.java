package com.example.personalfinance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    protected DebtPayment() {
    }

    public DebtPayment(Debt debt,
                       Account account,
                       BigDecimal amount,
                       LocalDateTime paidAt,
                       String note) {
        this.debt = debt;
        this.account = account;
        this.amount = amount;
        this.paidAt = paidAt;
        this.note = note;
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
