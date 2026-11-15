package com.example.personalfinance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, length = 255)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private boolean isSystemCategory;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    public boolean isSystemCategory() {
        return isSystemCategory;
    }

    public User getUser() {
        return user;
    }

    public Category(String name, TransactionType type, User user) {
        this.name = name;
        this.type = type;
        this.user = user;
        this.isSystemCategory = false;
        this.status = CategoryStatus.ACTIVE;
    }

    public Category(String name, TransactionType type, User user, boolean isSystemCategory) {
        this.name = name;
        this.type = type;
        this.user = user;
        this.isSystemCategory = isSystemCategory;
        this.status = CategoryStatus.ACTIVE;
    }

    protected Category() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TransactionType getType() {
        return type;
    }

    public CategoryStatus getStatus() {
        return status;
    }

    public void archive() {
        this.status = CategoryStatus.ARCHIVED;
    }

    public void activate() {
        this.status = CategoryStatus.ACTIVE;
    }
}
