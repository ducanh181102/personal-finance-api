package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.debtpayment.DebtPaymentResponse;
import com.example.personalfinance.model.DebtPayment;
import org.springframework.stereotype.Component;

@Component
public class DebtPaymentMapper {
    public DebtPaymentResponse toDebtPaymentResponse(DebtPayment debtPayment) {
        return new DebtPaymentResponse(
                debtPayment.getId(),
                debtPayment.getDebt().getId(),
                debtPayment.getAccount().getId(),
                debtPayment.getAmount(),
                debtPayment.getPaidAt(),
                debtPayment.getNote()
        );
    }
}
