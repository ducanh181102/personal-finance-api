package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Account(String name, String type, BigDecimal openingBalance, User user) {
        this.name = name;
        this.type = type;
        this.openingBalance = openingBalance;
        this.user = user;
        this.status = AccountStatus.ACTIVE;
    }

    protected Account() {
        // Required by JPA when it creates an entity from a database row.
    }

    public User getUser() {
        return user;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void archive() {
        this.status = AccountStatus.ARCHIVED;
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }
}
