package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(name = "budget_month", nullable = false)
    private LocalDate month;
    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    public Budget(Category category, LocalDate month, BigDecimal limitAmount) {
        this.category = category;
        this.month = month;
        this.limitAmount = limitAmount;
    }

    protected Budget() {
    }

    public int getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDate getMonth() {
        return month;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }
}
