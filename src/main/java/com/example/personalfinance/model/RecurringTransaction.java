package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = true, length = 255)
    private String note;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency frequency;
    @Column(nullable = false)
    private LocalDate nextRunDate;
    @Column(nullable = false)
    private boolean active = true;

    protected RecurringTransaction() {
    }

    public RecurringTransaction(Account account, Category category, BigDecimal amount, String note, RecurrenceFrequency frequency, LocalDate nextRunDate) {
        this.account = account;
        this.category = category;
        this.amount = amount;
        this.note = note;
        this.frequency = frequency;
        this.nextRunDate = nextRunDate;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setFrequency(RecurrenceFrequency frequency) {
        this.frequency = frequency;
    }

    public void setNextRunDate(LocalDate nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public boolean isActive() {
        return active;
    }
}
