package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.SavingsGoal;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.GoalContributionRepository;
import com.example.personalfinance.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SavingsGoalService {
    private final SavingsGoalRepository savingsGoalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final CurrentUserService currentUserService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              GoalContributionRepository goalContributionRepository,
                              CurrentUserService currentUserService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.goalContributionRepository = goalContributionRepository;
        this.currentUserService = currentUserService;
    }

    private SavingsGoal getOwnedSavingsGoalById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<SavingsGoal> savingsGoal = savingsGoalRepository.findById(id);
        if (savingsGoal.isEmpty()) throw new NotFoundException(id);
        if (savingsGoal.get().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return savingsGoal.get();
    }

    public List<SavingsGoal> getAllSavingsGoal() {
        return savingsGoalRepository.findByUserId(currentUserService.getCurrentUser().getId());
    }

    public SavingsGoal getSavingsGoal(int id) {
        return getOwnedSavingsGoalById(id);
    }

    public SavingsGoal createSavingsGoal(String name, BigDecimal targetAmount, LocalDate deadline) {
        SavingsGoal savingsGoal = new SavingsGoal(currentUserService.getCurrentUser(), name, targetAmount, deadline);
        return savingsGoalRepository.save(savingsGoal);
    }

    public SavingsGoal updateSavingsGoalById(int id, String name, BigDecimal targetAmount, LocalDate deadline) {
        SavingsGoal savingsGoal = getSavingsGoal(id);
        savingsGoal.setName(name != null ? name : savingsGoal.getName());
        savingsGoal.setTargetAmount(targetAmount != null ? targetAmount : savingsGoal.getTargetAmount());
        savingsGoal.setDeadline(deadline != null ? deadline : savingsGoal.getDeadline());
        savingsGoal.setCompleted(
                savingsGoal.getCurrentAmount().compareTo(savingsGoal.getTargetAmount()) >= 0
        );
        return savingsGoalRepository.save(savingsGoal);
    }

    public void deleteSavingsGoalById(int id) {
        SavingsGoal savingsGoal = getSavingsGoal(id);
        if (goalContributionRepository.existsByGoalId(id)) {
            throw new IllegalArgumentException("Không được xóa mục tiêu đã có khoản góp.");
        }
        savingsGoalRepository.delete(savingsGoal);
    }

    public void handleAddContributionAmount(SavingsGoal savingsGoal, BigDecimal amount) {
        savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().add(amount));
        savingsGoal.setCompleted(savingsGoal.getCurrentAmount().compareTo(savingsGoal.getTargetAmount()) >= 0);
        savingsGoalRepository.save(savingsGoal);
    }

    public void reverseContribution(SavingsGoal savingsGoal, BigDecimal amount) {
        savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().subtract(amount));
        savingsGoal.setCompleted(savingsGoal.getCurrentAmount().compareTo(savingsGoal.getTargetAmount()) >= 0);
        savingsGoalRepository.save(savingsGoal);
    }
}
