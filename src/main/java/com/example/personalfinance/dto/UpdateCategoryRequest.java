package com.example.personalfinance.dto;

import com.example.personalfinance.model.TransactionType;
import jakarta.validation.constraints.Size;

public class UpdateCategoryRequest {
    @Size(max = 255, message = "Tên danh mục không vượt quá 255 kí tự")
    private final String name;
    private final TransactionType type;

    public UpdateCategoryRequest(String name, TransactionType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TransactionType getType() {
        return type;
    }
}
