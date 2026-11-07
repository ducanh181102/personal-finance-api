package com.example.personalfinance.dto;

import com.example.personalfinance.model.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateTransactionRequest {
    @NotNull(message = "ID tài khoản không được để trống")
    @Positive(message = "ID tài khoản phải lớn hơn 0")
    private Integer accountId;
    @NotNull(message = "Loại giao dịch không được trống")
    private TransactionType type;
    @NotNull(message = "Số tiền giao dịch không được để trống")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
    @Positive(message = "Số tiền giao dịch phải lớn hơn 0")
    private BigDecimal amount;
    @Size(max = 255, message = "Ghi chú tối đa 255 kí tự")
    private String note;

    public CreateTransactionRequest() {
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setNote(String note) {
        this.note = note;
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
