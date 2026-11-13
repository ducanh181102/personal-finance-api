package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.transaction.TransactionResponse;
import com.example.personalfinance.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getCategory().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getNote()
        );
    }
}
