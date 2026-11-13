package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.GoalContribution;
import com.example.personalfinance.model.SavingsGoal;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.GoalContributionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GoalContributionService {
    private final GoalContributionRepository goalContributionRepository;
    private final CurrentUserService currentUserService;
    private final SavingsGoalService savingsGoalService;
    private final AccountService accountService;

    public GoalContributionService(GoalContributionRepository goalContributionRepository, CurrentUserService currentUserService, SavingsGoalService savingsGoalService, AccountService accountService) {
        this.goalContributionRepository = goalContributionRepository;
        this.currentUserService = currentUserService;
        this.savingsGoalService = savingsGoalService;
        this.accountService = accountService;
    }

    private GoalContribution getOwnedGoalContributionById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<GoalContribution> goalContribution = goalContributionRepository.findById(id);
        if (goalContribution.isEmpty()) throw new NotFoundException(id);
        if (goalContribution.get().getAccount().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return goalContribution.get();
    }

    public List<GoalContribution> getAllGoalContributions() {
        return goalContributionRepository.findByAccountUserId(currentUserService.getCurrentUser().getId());
    }

    public GoalContribution getGoalContributionById(int id) {
        return getOwnedGoalContributionById(id);
    }

    @Transactional
    public GoalContribution createGoalContribution(int savingsGoalId, int accountId, BigDecimal amount, LocalDateTime contributedAt, String note) {
        SavingsGoal goal = savingsGoalService.getSavingsGoal(savingsGoalId);
        Account account = accountService.getAccountById(accountId);

        GoalContribution goalContribution = new GoalContribution(goal, account, amount, contributedAt, note);

        // Cập nhật Goal
        savingsGoalService.handleAddContributionAmount(goal, amount);

        return goalContributionRepository.save(goalContribution);
    }

    @Transactional
    public void deleteGoalContributionById(int id) {
        GoalContribution goalContribution = getGoalContributionById(id);
        goalContributionRepository.delete(goalContribution);
        // Cập nhật Goal
        savingsGoalService.reverseContribution(goalContribution.getGoal(), goalContribution.getAmount());
    }

}
