package com.example.personalfinance.repository;

import com.example.personalfinance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByAccountId(int accountId);

    List<Transaction> findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(
            int accountId,
            LocalDateTime from,
            LocalDateTime toExclusive);
}
