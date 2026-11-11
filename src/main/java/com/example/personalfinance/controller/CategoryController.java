package com.example.personalfinance.controller;

import com.example.personalfinance.dto.CategoryResponse;
import com.example.personalfinance.dto.CreateCategoryRequest;
import com.example.personalfinance.dto.UpdateCategoryRequest;
import com.example.personalfinance.mapper.CategoryMapper;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getCategories().stream().map(categoryMapper::toCategoryResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable("id") Integer id) {
        Category category = categoryService.findCategoryById(id);
        return ResponseEntity.ok(categoryMapper.toCategoryResponse(category));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.createCategory(request.getName(), request.getType());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryMapper.toCategoryResponse(category));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable("id") int id, @Valid @RequestBody UpdateCategoryRequest request) {
        Category category = categoryService.updateCategory(id, request.getName(), request.getType());
        return ResponseEntity.ok(categoryMapper.toCategoryResponse(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") int id) {
        categoryService.deleteCategoryById(id);
        return ResponseEntity.noContent().build();
    }
}
