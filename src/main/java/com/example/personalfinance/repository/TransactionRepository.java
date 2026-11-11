package com.example.personalfinance.repository;

import com.example.personalfinance.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByAccountId(int accountId);

    List<Transaction> findByCategoryId(int categoryId);

    Page<Transaction> findByAccountId(int accountId, Pageable pageable);

    List<Transaction> findByAccountUserId(int userId);

    Page<Transaction> findByAccountUserId(int userId, Pageable pageable);

    boolean existsByCategoryId(int categoryId);

    List<Transaction> findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int accountId, LocalDateTime from, LocalDateTime toExclusive);

    List<Transaction> findByCategoryIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int categoryId, LocalDateTime from, LocalDateTime toExclusive);

    Page<Transaction> findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int accountId, LocalDateTime from, LocalDateTime toExclusive, Pageable pageable);

    boolean existsByAccountId(int accountId);
}
