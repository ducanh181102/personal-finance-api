package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.savingsgoal.SavingsGoalResponse;
import com.example.personalfinance.model.SavingsGoal;
import org.springframework.stereotype.Component;

@Component
public class SavingsGoalMapper {
    public SavingsGoalResponse toSavingsGoalResponse(SavingsGoal savingsGoal) {
        return new SavingsGoalResponse(
                savingsGoal.getId(),
                savingsGoal.getUser().getId(),
                savingsGoal.getName(),
                savingsGoal.getTargetAmount(),
                savingsGoal.getCurrentAmount(),
                savingsGoal.getDeadline(),
                savingsGoal.isCompleted()
        );
    }
}
