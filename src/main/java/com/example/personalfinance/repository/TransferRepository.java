package com.example.personalfinance.repository;

import com.example.personalfinance.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Integer> {
    List<Transfer> findByFromAccountUserId(int id);
    List<Transfer> findByFromAccountIdAndFromAccountUserId(int fromAccountId, int userId);
    List<Transfer> findByToAccountIdAndToAccountUserId(int fromAccountId, int userId);
    boolean existsByFromAccountIdOrToAccountId(int fromAccountId, int toAccountId);
}
