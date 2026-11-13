package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.recurring.RecurringTransactionResponse;
import com.example.personalfinance.model.RecurringTransaction;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionMapper {
    public RecurringTransactionResponse toRecurringTransactionResponse(RecurringTransaction recurringTransaction) {
        return new RecurringTransactionResponse(
                recurringTransaction.getId(),
                recurringTransaction.getAccount().getId(),
                recurringTransaction.getCategory().getId(),
                recurringTransaction.getAmount(),
                recurringTransaction.getNote(),
                recurringTransaction.getFrequency(),
                recurringTransaction.getNextRunDate(),
                recurringTransaction.isActive()
        );
    }
}
