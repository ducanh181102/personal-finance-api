package com.example.personalfinance.service;

import com.example.personalfinance.dto.BudgetSummaryResponse;
import com.example.personalfinance.dto.MonthlyDashboardResponse;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BudgetService budgetService;
    private final CurrentUserService currentUserService;

    public DashboardService(TransactionRepository transactionRepository, AccountRepository accountRepository, BudgetService budgetService, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.budgetService = budgetService;
        this.currentUserService = currentUserService;
    }

    private BigDecimal getTotal(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public MonthlyDashboardResponse getMonthlyDashboard(LocalDate month) {
        if (month.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("Phải là ngày đầu của tháng.");
        }

        User currentUser = currentUserService.getCurrentUser();
        LocalDateTime start = month.atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atStartOfDay();

        List<Transaction> transactions = transactionRepository.findByAccountUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(currentUser.getId(), start, end);
        List<BudgetSummaryResponse> budgets = budgetService.getBudgetSummaryByMonth(month);

        BigDecimal totalIncome = getTotal(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = getTotal(transactions, TransactionType.EXPENSE);
        BigDecimal netChange = totalIncome.subtract(totalExpense);
        int accountCount = accountRepository.findByUserId(currentUser.getId()).size();
        int budgetCount = budgets.size();
        int overBudgetCount = Math.toIntExact(budgets.stream()
                .filter(BudgetSummaryResponse::isOverBudget)
                .count());

        return new MonthlyDashboardResponse(
                month,
                totalIncome,
                totalExpense,
                netChange,
                accountCount,
                budgetCount,
                overBudgetCount
        );
    }
}
