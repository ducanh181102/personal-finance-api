package com.example.personalfinance.repository;

import com.example.personalfinance.model.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Integer> {
    List<DebtPayment> findByAccountUserId(int userId);
    boolean existsByDebtId(int debtId);
    boolean existsByAccountId(int accountId);
}
