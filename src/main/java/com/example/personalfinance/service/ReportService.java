package com.example.personalfinance.service;

import com.example.personalfinance.dto.category.CategorySummaryResponse;
import com.example.personalfinance.dto.report.MonthlyCashFlowReport;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public ReportService(TransactionRepository transactionRepository, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public MonthlyCashFlowReport getMonthlyReport(LocalDate month) {
        if (month.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("Phải là ngày đầu của tháng.");
        }

        int userId = currentUserService.getCurrentUser().getId();
        return new MonthlyCashFlowReport(summarize(userId, month), summarize(userId, month.minusMonths(1)));
    }

    private MonthlyCashFlowReport.Period summarize(int userId, LocalDate month) {
        List<CategorySummaryResponse> categories = transactionRepository.summarizeCategoriesByUserId(
                userId, month.atStartOfDay(), month.plusMonths(1).atStartOfDay()
        );
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (CategorySummaryResponse category : categories) {
            if (category.getType() == TransactionType.INCOME) {
                income = income.add(category.getTotalAmount());
            } else {
                expense = expense.add(category.getTotalAmount());
            }
        }

        return new MonthlyCashFlowReport.Period(month, income, expense, income.subtract(expense), categories);
    }
}
