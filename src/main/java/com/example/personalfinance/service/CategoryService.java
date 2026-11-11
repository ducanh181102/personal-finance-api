package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.User;
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
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository, CurrentUserService currentUserService, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    private Category getOwnedCategoryById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) throw new NotFoundException(id);
        if (category.get().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return category.get();
    }

    // Đình chỉ
//    public List<Category> getAllCategories() {
//        return categoryRepository.findAll();
//    }

    public Category createCategory(String name, TransactionType type) {
        User user = currentUserService.getCurrentUser();
        Category category = new Category(name, type, user);
        return categoryRepository.save(category);
    }

    public Category findCategoryById(int id) {
        return getOwnedCategoryById(id);
    }

    @Transactional
    public Category updateCategory(int id, String name, TransactionType type) {
        Category category = getOwnedCategoryById(id);
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
        Category category = getOwnedCategoryById(id);
        if (transactionRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Không được xóa category đang có transaction sử dụng.");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public List<Category> getCategories() {
        User currentUser = currentUserService.getCurrentUser();
        return categoryRepository.findByUserId(currentUser.getId());
    }
}
