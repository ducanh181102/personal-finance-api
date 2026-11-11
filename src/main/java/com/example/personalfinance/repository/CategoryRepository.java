package com.example.personalfinance.repository;

import com.example.personalfinance.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByUserId(Integer userId);
}
