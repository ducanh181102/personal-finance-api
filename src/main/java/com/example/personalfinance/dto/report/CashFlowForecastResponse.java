package com.example.personalfinance.dto.report;

import com.example.personalfinance.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowForecastResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal startingBalance,
        BigDecimal endingBalance,
        LocalDate firstShortfallDate,
        List<Day> days
) {
    public record Day(
            LocalDate date,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal projectedBalance,
            List<Event> events
    ) {}

    public record Event(
            Source source,
            int sourceId,
            TransactionType type,
            BigDecimal amount,
            String label,
            LocalDate scheduledDate,
            Integer accountId
    ) {}

    public enum Source {
        RECURRING,
        DEBT
    }
}
