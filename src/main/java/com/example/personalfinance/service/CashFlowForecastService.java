package com.example.personalfinance.service;

import com.example.personalfinance.dto.report.CashFlowForecastResponse;
import com.example.personalfinance.dto.report.CashFlowForecastResponse.Day;
import com.example.personalfinance.dto.report.CashFlowForecastResponse.Event;
import com.example.personalfinance.dto.report.CashFlowForecastResponse.Source;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtStatus;
import com.example.personalfinance.model.DebtType;
import com.example.personalfinance.model.RecurringTransaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.DebtRepository;
import com.example.personalfinance.repository.RecurringTransactionRepository;
import com.example.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CashFlowForecastService {
    private static final ZoneId FORECAST_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Comparator<Event> EVENT_ORDER = Comparator.comparing(Event::scheduledDate)
            .thenComparing(Event::source)
            .thenComparingInt(Event::sourceId);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public CashFlowForecastService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            RecurringTransactionRepository recurringTransactionRepository,
            DebtRepository debtRepository,
            CurrentUserService currentUserService
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    // Đọc số dư và ngày chạy kế tiếp cùng một snapshot khi lịch định kỳ có thể chạy đồng thời.
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public CashFlowForecastResponse getForecast(int days) {
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("Số ngày dự báo phải từ 1 đến 365.");
        }

        int userId = currentUserService.getCurrentUser().getId();
        LocalDate from = LocalDate.now(FORECAST_ZONE);
        LocalDate to = from.plusDays(days - 1L);

        // Chuyển tiền nội bộ có tổng bằng 0 khi cộng số dư của toàn bộ ví.
        BigDecimal startingBalance = accountRepository.sumOpeningBalancesByUserId(userId)
                .add(transactionRepository.sumNetAmountByUserId(userId));
        Map<LocalDate, List<Event>> eventsByDate = new HashMap<>();
        for (RecurringTransaction recurring : recurringTransactionRepository
                .findByAccountUserIdAndActiveTrueAndNextRunDateLessThanEqual(userId, to)) {
            String label = recurring.getNote() == null || recurring.getNote().isBlank()
                    ? recurring.getCategory().getName() : recurring.getNote();
            for (LocalDate date = recurring.getNextRunDate(); !date.isAfter(to);
                 date = recurring.getFrequency().nextDateAfter(date)) {
                addEvent(eventsByDate, from, new Event(
                        Source.RECURRING, recurring.getId(), recurring.getCategory().getType(),
                        recurring.getAmount(), label, date, recurring.getAccount().getId()
                ));
            }
        }
        for (Debt debt : debtRepository.findByUserIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(userId, DebtStatus.OPEN, to)) {
            TransactionType type = debt.getType() == DebtType.BORROWED ? TransactionType.EXPENSE : TransactionType.INCOME;
            addEvent(eventsByDate, from, new Event(
                    Source.DEBT, debt.getId(), type, debt.getRemainingAmount(), debt.getPersonName(), debt.getDueDate(), null
            ));
        }

        List<Day> forecast = new ArrayList<>(days);
        BigDecimal balance = startingBalance;
        LocalDate firstShortfallDate = null;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<Event> events = eventsByDate.getOrDefault(date, List.of()).stream().sorted(EVENT_ORDER).toList();
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (Event event : events) {
                if (event.type() == TransactionType.INCOME) {
                    income = income.add(event.amount());
                } else {
                    expense = expense.add(event.amount());
                }
            }
            balance = balance.add(income).subtract(expense);
            if (firstShortfallDate == null && balance.signum() < 0) {
                firstShortfallDate = date;
            }
            forecast.add(new Day(date, income, expense, balance, events));
        }
        return new CashFlowForecastResponse(from, to, startingBalance, balance, firstShortfallDate, forecast);
    }

    private void addEvent(Map<LocalDate, List<Event>> eventsByDate, LocalDate from, Event event) {
        // Giữ ngày gốc để phân biệt khoản quá hạn với khoản đến hạn hôm nay.
        LocalDate forecastDate = event.scheduledDate().isBefore(from) ? from : event.scheduledDate();
        eventsByDate.computeIfAbsent(forecastDate, date -> new ArrayList<>()).add(event);
    }
}
