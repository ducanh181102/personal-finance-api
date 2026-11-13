package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;
    @Column(precision = 19, scale = 2)
    private BigDecimal currentAmount;
    @Column(nullable = false)
    private LocalDate deadline;
    @Column(nullable = false)
    private boolean completed;

    protected SavingsGoal() {
    }

    public SavingsGoal(User user, String name, BigDecimal targetAmount, LocalDate deadline) {
        this.user = user;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = BigDecimal.ZERO;
        this.deadline = deadline;
        this.completed = false;
    }

    public int getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public void setName(String name) {
        this.name = name;
    }
}
