package com.example.personalfinance.dto.category;

import com.example.personalfinance.model.TransactionType;

public class CategoryResponse {
    private final int id;
    private final String name;
    private final TransactionType type;
    private final int userId;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TransactionType getType() {
        return type;
    }

    public int getUserId() {
        return userId;
    }

    public CategoryResponse(int id, String name, TransactionType type, int userId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.userId = userId;
    }
}
