package com.example.personalfinance.repository;

import com.example.personalfinance.model.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Integer> {
    List<GoalContribution> findByAccountUserId(int id);

    boolean existsByGoalId(int goalId);
}
