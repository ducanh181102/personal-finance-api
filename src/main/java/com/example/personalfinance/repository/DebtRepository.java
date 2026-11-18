package com.example.personalfinance.repository;

import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Integer> {
    List<Debt> findByUserId(int userId);

    List<Debt> findByUserIdAndStatus(int userId, DebtStatus status);

    List<Debt> findByUserIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(
            int userId,
            DebtStatus status,
            LocalDate dueDate
    );
}
