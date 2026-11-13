package com.example.personalfinance.dto.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResponse {
    private final int id;
    private final int fromAccountId;
    private final int toAccountId;
    private final BigDecimal amount;
    private final LocalDateTime transferDate;
    private final String note;

    public TransferResponse(int id, int fromAccountId, int toAccountId, BigDecimal amount, LocalDateTime transferDate, String note) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.transferDate = transferDate;
        this.note = note;
    }

    public int getToAccountId() {
        return toAccountId;
    }

    public int getFromAccountId() {
        return fromAccountId;
    }

    public int getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTransferDate() {
        return transferDate;
    }

    public String getNote() {
        return note;
    }
}
