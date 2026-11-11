package com.example.personalfinance.scheduler;

import com.example.personalfinance.service.RecurringTransactionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionScheduler(
            RecurringTransactionService recurringTransactionService
    ) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @Scheduled(
            cron = "0 5 0 * * *",
            zone = "Asia/Ho_Chi_Minh"
    )
    public void runEveryDay() {
        recurringTransactionService.runDueRecurringTransactions();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterApplicationStarts() {
        recurringTransactionService.runDueRecurringTransactions();
    }
}