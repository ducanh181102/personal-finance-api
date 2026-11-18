package com.example.personalfinance.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TransactionSource source = TransactionSource.MANUAL;

    public Transaction(Account account, TransactionType type, BigDecimal amount, LocalDateTime transactionDate, String note, Category category) {
        this(account, type, amount, transactionDate, note, category, TransactionSource.MANUAL);
    }

    public Transaction(Account account, TransactionType type, BigDecimal amount, LocalDateTime transactionDate, String note, Category category, TransactionSource source) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.note = note;
        this.category = category;
        this.source = source;
    }

    public Category getCategory() {
        return category;
    }

    public TransactionSource getSource() {
        return source;
    }

    public boolean isManual() {
        return source == null || source == TransactionSource.MANUAL;
    }

    protected Transaction() {
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

    public void setNote(String note) {
        this.note = note;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
