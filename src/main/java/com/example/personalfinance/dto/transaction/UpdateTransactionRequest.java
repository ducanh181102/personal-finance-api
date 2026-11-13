package com.example.personalfinance.dto.transaction;

import com.example.personalfinance.model.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateTransactionRequest {
    private final TransactionType type;
    @Positive(message = "Số tiền phải là số dương")
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân"
    )
    private final BigDecimal amount;
    @Size(max = 255, message = "Ghi chú tối đa là 255 kí tự")
    private final String note;

    public UpdateTransactionRequest(TransactionType type, BigDecimal amount, String note) {
        this.type = type;
        this.amount = amount;
        this.note = note;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }
}
