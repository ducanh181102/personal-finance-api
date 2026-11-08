package com.example.personalfinance.repository;

import com.example.personalfinance.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Integer> {
    boolean existsByCategoryIdAndMonth(int category_id, LocalDate month);
    List<Budget> findByMonth(LocalDate month);
}
