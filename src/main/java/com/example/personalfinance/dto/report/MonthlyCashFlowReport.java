package com.example.personalfinance.dto.report;

import com.example.personalfinance.dto.category.CategorySummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyCashFlowReport(Period current, Period previous) {
    public record Period(
            LocalDate month,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netChange,
            List<CategorySummaryResponse> categories
    ) {}
}
