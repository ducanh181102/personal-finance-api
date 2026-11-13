package com.example.personalfinance.repository;

import com.example.personalfinance.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Integer> {
    List<Debt> findByUserId(int userId);
}
