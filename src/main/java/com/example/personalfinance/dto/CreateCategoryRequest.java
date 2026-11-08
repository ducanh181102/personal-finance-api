package com.example.personalfinance.dto;

import com.example.personalfinance.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateCategoryRequest {
    @Size(max = 255, message = "Tên danh mục không vượt quá 255 kí tự")
    @NotBlank(message = "Tên danh mục không được trống")
    private String name;
    @NotNull(message = "Loại danh mục không được để ")
    private TransactionType type;

    public CreateCategoryRequest(String name, TransactionType type) {
        this.name = name;
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TransactionType getType() {
        return type;
    }
}
