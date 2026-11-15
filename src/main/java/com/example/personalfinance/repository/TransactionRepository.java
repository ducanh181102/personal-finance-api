package com.example.personalfinance.repository;

import com.example.personalfinance.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer>,
        JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByAccountId(int accountId);

    List<Transaction> findByCategoryId(int categoryId);

    List<Transaction> findByAccountUserId(int userId);

    boolean existsByCategoryId(int categoryId);

    List<Transaction> findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int accountId, LocalDateTime from, LocalDateTime toExclusive);

    List<Transaction> findByCategoryIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int categoryId, LocalDateTime from, LocalDateTime toExclusive);

    Page<Transaction> findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int accountId, LocalDateTime from, LocalDateTime toExclusive, Pageable pageable);

    boolean existsByAccountId(int accountId);

    List<Transaction> findByAccountUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(
            int userId,
            LocalDateTime from,
            LocalDateTime toExclusive
    );
}
