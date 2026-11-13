package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.goalcontribution.GoalContributionResponse;
import com.example.personalfinance.model.GoalContribution;
import org.springframework.stereotype.Component;

@Component
public class GoalContributionMapper {
    public GoalContributionResponse toGoalContributionResponse(GoalContribution goalContribution) {
        return new GoalContributionResponse(
                goalContribution.getId(),
                goalContribution.getGoal().getId(),
                goalContribution.getAccount().getId(),
                goalContribution.getAmount(),
                goalContribution.getContributedAt(),
                goalContribution.getNote()
        );
    }
}
