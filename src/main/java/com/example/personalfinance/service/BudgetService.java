package com.example.personalfinance.service;

import com.example.personalfinance.dto.BudgetSummaryResponse;
import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.*;
import com.example.personalfinance.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BudgetService {

    public BudgetService(BudgetRepository budgetRepository, CategoryService categoryService, TransactionService transactionService, CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    private Budget getOwnedBudgetById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Budget> budget = budgetRepository.findById(id);
        if (budget.isEmpty()) throw new NotFoundException(id);
        if (budget.get().getCategory().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return budget.get();
    }

    @Transactional
    public Budget createBudget(int categoryId, LocalDate month, BigDecimal limitAmount) {
        Category category = categoryService.findCategoryById(categoryId);
        if (category.getType() != TransactionType.EXPENSE)
            throw new IllegalArgumentException("Chỉ có loại danh mục chi tiêu mới tạo được ngân sách.");
        if (budgetRepository.existsByCategoryIdAndMonth(categoryId, month))
            throw new IllegalArgumentException("Không được tạo ngân sách có cùng danh mục và tháng.");
        if (!month.equals(month.withDayOfMonth(1))) {
            throw new IllegalArgumentException(
                    "Tháng ngân sách phải là ngày đầu tháng"
            );
        }
        Budget budget = new Budget(category, month, limitAmount);
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget updateBudgetById(int id, BigDecimal limitAmount) {
        Budget budget = getOwnedBudgetById(id);
        budget.setLimitAmount(limitAmount);
        return budgetRepository.save(budget);
    }

    public List<Budget> getAllBudgets() {
        User currentUser = currentUserService.getCurrentUser();
        return budgetRepository.findByCategoryUserId(currentUser.getId());
    }

    public Budget getBudgetById(int id) {
        return getOwnedBudgetById(id);
    }

    @Transactional
    public void deleteBudgetById(int id) {
        Budget budget = getOwnedBudgetById(id);
        budgetRepository.delete(budget);
    }

    public BudgetSummaryResponse getBudgetSummary(int id) {

        Budget budget = getOwnedBudgetById(id);

        LocalDate from = budget.getMonth();
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        // Lấy danh sách giao dịch
        List<Transaction> transactions = transactionService.findByCategoryIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(budget.getCategory().getId(), from, to);

        BigDecimal spentAmount = transactions.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = budget.getLimitAmount().subtract(spentAmount);

        BigDecimal usagePercentage = spentAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        budget.getLimitAmount(),
                        2,
                        RoundingMode.HALF_UP
                );

        boolean overBudget =
                spentAmount.compareTo(budget.getLimitAmount()) > 0;

        return new BudgetSummaryResponse(budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getMonth(),
                budget.getLimitAmount(),
                spentAmount,
                remainingAmount,
                usagePercentage,
                overBudget
        );
    }

    public List<BudgetSummaryResponse> getBudgetSummaryByMonth(LocalDate month) {
        if (month.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("Phải là ngày đầu của tháng.");
        }
        List<Budget> budgets = budgetRepository.findByCategoryUserIdAndMonth(currentUserService.getCurrentUser().getId(), month);
        return budgets.stream().map(budget -> getBudgetSummary(budget.getId())).toList();
    }
}
