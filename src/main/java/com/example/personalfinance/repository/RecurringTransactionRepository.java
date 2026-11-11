package com.example.personalfinance.repository;

import com.example.personalfinance.model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Integer> {
    List<RecurringTransaction> findByAccountUserId(int userId);

    boolean existsByAccountId(int accountId);

    boolean existsByCategoryId(int categoryId);

    List<RecurringTransaction> findByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
}
