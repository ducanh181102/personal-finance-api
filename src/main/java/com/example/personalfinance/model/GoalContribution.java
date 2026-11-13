package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal_contributions")
public class GoalContribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "savings_goal_id", nullable = false)
    private SavingsGoal goal;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Column(nullable = false, scale = 2, precision = 19)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDateTime contributedAt;
    @Column(nullable = true, length = 255)
    private String note;

    protected GoalContribution() {
    }

    public GoalContribution(SavingsGoal goal, Account account, BigDecimal amount, LocalDateTime contributedAt, String note) {
        this.goal = goal;
        this.account = account;
        this.amount = amount;
        this.contributedAt = contributedAt;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public SavingsGoal getGoal() {
        return goal;
    }

    public Account getAccount() {
        return account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getContributedAt() {
        return contributedAt;
    }

    public String getNote() {
        return note;
    }
}
