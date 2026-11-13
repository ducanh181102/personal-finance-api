package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.category.CategoryResponse;
import com.example.personalfinance.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getUser().getId()
        );
    }
}
