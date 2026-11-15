package com.example.personalfinance.repository;

import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.CategoryStatus;
import com.example.personalfinance.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByUserId(Integer userId);

    List<Category> findByUserIdAndStatus(Integer userId, CategoryStatus categoryStatus);

    Optional<Category> findByNameAndTypeAndUserIdAndIsSystemCategoryTrue(
            String name,
            TransactionType transactionType,
            int userId
    );

    boolean existsByNameAndTypeAndUserIdAndIsSystemCategoryTrue(
            String name,
            TransactionType transactionType,
            int userId
    );

}
