package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.debt.DebtResponse;
import com.example.personalfinance.model.Debt;
import org.springframework.stereotype.Component;

@Component
public class DebtMapper {
    public DebtResponse toDebtResponse(Debt debt) {
        return new DebtResponse(
                debt.getId(),
                debt.getPersonName(),
                debt.getType(),
                debt.getOriginalAmount(),
                debt.getRemainingAmount(),
                debt.getDueDate(),
                debt.getStatus(),
                debt.getNote()
        );
    }
}
