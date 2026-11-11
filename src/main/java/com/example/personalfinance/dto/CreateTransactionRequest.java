package com.example.personalfinance.dto;

import com.example.personalfinance.model.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateTransactionRequest {
    @NotNull(message = "ID tài khoản không được để trống")
    @Positive(message = "ID tài khoản phải lớn hơn 0")
    private final Integer accountId;
    @NotNull(message = "ID danh mục không được để trống")
    @Positive(message = "ID danh mục phải lớn hơn 0")
    private final Integer categoryId;
    @NotNull(message = "Loại giao dịch không được trống")
    private final TransactionType type;
    @NotNull(message = "Số tiền giao dịch không được để trống")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
    @Positive(message = "Số tiền giao dịch phải lớn hơn 0")
    private final BigDecimal amount;
    @Size(max = 255, message = "Ghi chú tối đa 255 kí tự")
    private final String note;
    @NotNull(message = "Thời gian giao dịch không được để trống")
    @PastOrPresent(message = "Thời gian giao dịch không được ở tương lai")
    private final LocalDateTime transactionDate;

    public CreateTransactionRequest(Integer accountId, Integer categoryId, TransactionType type, BigDecimal amount, String note, LocalDateTime transactionDate) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public Integer getAccountId() {
        return accountId;
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
