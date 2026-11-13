package com.example.personalfinance.repository;

import com.example.personalfinance.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Integer> {
    List<SavingsGoal> findByUserId(int userId);
}
