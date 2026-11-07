package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDateTime transactionDate;
    @Column(length = 255)
    private String note;

    protected Transaction() {
    }

    public Transaction(Account account, TransactionType type, BigDecimal amount, LocalDateTime transactionDate, String note) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public String getNote() {
        return note;
    }
}
