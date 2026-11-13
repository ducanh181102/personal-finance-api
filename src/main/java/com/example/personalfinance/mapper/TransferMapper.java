package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.transfer.TransferResponse;
import com.example.personalfinance.model.Transfer;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {
    public TransferResponse toTransferResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(),
                transfer.getAmount(),
                transfer.getTransferDate(),
                transfer.getNote()
        );
    }
}
