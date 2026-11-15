package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.*;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.RecurringTransactionRepository;
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
    private final RecurringTransactionRepository recurringTransactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository,
                           CurrentUserService currentUserService,
                           RecurringTransactionRepository recurringTransactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.recurringTransactionRepository = recurringTransactionRepository;
    }

    private Category getOwnedCategoryById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) throw new NotFoundException(id);
        if (category.get().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return category.get();
    }

    public Category getActiveCategoryById(int id) {
        Category category = getOwnedCategoryById(id);
        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new IllegalArgumentException("Không thể sử dụng danh mục đã lưu trữ.");
        }
        return category;
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

    @Transactional
    public void ensureDebtSystemCategories(User user) {
        ensureSystemCategory("TRẢ NỢ", TransactionType.EXPENSE, user);
        ensureSystemCategory("THU HỒI NỢ", TransactionType.INCOME, user);
        ensureSystemCategory("HOÀN TRẢ NỢ", TransactionType.INCOME, user);
        ensureSystemCategory("HOÀN THU HỒI NỢ", TransactionType.EXPENSE, user);
    }

    private void ensureSystemCategory(String name, TransactionType type, User user) {
        if (!categoryRepository.existsByNameAndTypeAndUserIdAndIsSystemCategoryTrue(name, type, user.getId())) {
            categoryRepository.save(new Category(name, type, user, true));
        }
    }

    public Category findCategoryById(int id) {
        return getOwnedCategoryById(id);
    }

    @Transactional
    public Category updateCategory(int id, String name, TransactionType type) {
        Category category = getOwnedCategoryById(id);
        if (category.isSystemCategory()) throw new IllegalArgumentException("Không thể chỉnh sửa danh mục hệ thống.");
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
        if (category.isSystemCategory()) throw new IllegalArgumentException("Không thể xoá danh mục hệ thống.");
        if (transactionRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Không được xóa category đang có transaction sử dụng.");
        }

        if (recurringTransactionRepository.existsByCategoryId(id))
            throw new IllegalArgumentException("Không được xóa danh mục đang được dùng bởi giao dịch định kỳ.");

        categoryRepository.delete(category);
    }

    @Transactional
    public List<Category> getCategories(boolean includeArchived) {
        User currentUser = currentUserService.getCurrentUser();
        return includeArchived ? categoryRepository.findByUserId(currentUser.getId()) : categoryRepository.findByUserIdAndStatus(currentUser.getId(), CategoryStatus.ACTIVE);
    }

    @Transactional
    public void archiveCategoryById(int id) {
        Category category = findCategoryById(id);
        if (category.isSystemCategory()) {
            throw new IllegalArgumentException("Không thể lưu trữ danh mục hệ thống.");
        }
        category.archive();
        recurringTransactionRepository.findByCategoryIdAndActiveTrue(id)
                .forEach(recurringTransaction -> recurringTransaction.setActive(false));
        categoryRepository.save(category);
    }

    @Transactional
    public void activateCategoryById(int id) {
        Category category = findCategoryById(id);
        category.activate();
        categoryRepository.save(category);
    }
}
