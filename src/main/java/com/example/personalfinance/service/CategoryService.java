package com.example.personalfinance.service;

import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    public final CategoryRepository categoryRepository;
    public final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(String name, TransactionType type) {
        Category category = new Category(name, type);
        return categoryRepository.save(category);
    }

    public Category findCategoryById(int id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new NotFoundException(id);
        } else {
            return category.get();
        }
    }

    @Transactional
    public Category updateCategory(int id, String name, TransactionType type) {
        Category category = findCategoryById(id);
        category.setName(name != null ? name : category.getName());
        if (type != null && type != category.getType()) {
            if (transactionRepository.existsByCategoryId(id)) {
                throw new IllegalArgumentException("Không được đổi type của category đang có transaction, vì sẽ làm các transaction cũ lệch type.");
            }
            category.setType(type);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategoryById(int id) {
        Category category = findCategoryById(id);
        if (transactionRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Không được xóa category đang có transaction sử dụng.");
        }
        categoryRepository.delete(category);
    }
}
