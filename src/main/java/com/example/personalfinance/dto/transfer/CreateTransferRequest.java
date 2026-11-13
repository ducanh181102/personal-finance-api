package com.example.personalfinance.dto.transfer;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateTransferRequest {
    @NotNull(message = "ID tài khoản gửi không được trống.")
    @Positive(message = "ID tài khoản gửi phải lớn hơn 0")
    private final Integer fromAccountId;
    @NotNull(message = "ID tài khoản nhận không được trống.")
    @Positive(message = "ID tài khoản nhận phải lớn hơn 0")
    private final Integer toAccountId;
    @NotNull(message = "Số tiền không được trống.")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
    @Positive(message = "Số tiền giao dịch phải lớn hơn 0")
    private final BigDecimal amount;
    @NotNull(message = "Thời gian giao dịch không được để trống")
    @PastOrPresent(message = "Thời gian giao dịch không được ở tương lai")
    private final LocalDateTime transferDate;
    @Size(max = 255, message = "Ghi chú tối đa 255 kí tự")
    private final String note;

    public CreateTransferRequest(Integer fromAccountId, Integer toAccountId, BigDecimal amount, LocalDateTime transferDate, String note) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.transferDate = transferDate;
        this.note = note;
    }

    public Integer getFromAccountId() {
        return fromAccountId;
    }

    public Integer getToAccountId() {
        return toAccountId;
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
