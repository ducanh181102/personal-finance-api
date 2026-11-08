package com.example.personalfinance.service;

import com.example.personalfinance.dto.BudgetSummaryResponse;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Budget;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
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

    public BudgetService(BudgetRepository budgetRepository, CategoryService categoryService, TransactionService transactionService) {
        this.budgetRepository = budgetRepository;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

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
        Budget budget = getBudgetById(id);
        budget.setLimitAmount(limitAmount);
        return budgetRepository.save(budget);
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public Budget getBudgetById(int id) {
        Optional<Budget> budget = budgetRepository.findById(id);
        if (budget.isEmpty()) throw new NotFoundException(id);
        else return budget.get();
    }

    @Transactional
    public void deleteBudgetById(int id) {
        Budget budget = getBudgetById(id);
        budgetRepository.delete(budget);
    }

    public BudgetSummaryResponse getBudgetSummary(int id) {

        Budget budget = getBudgetById(id);

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
        List<Budget> budgets = budgetRepository.findByMonth(month);
        return budgets.stream().map(budget -> getBudgetSummary(budget.getId())).toList();
    }

}
