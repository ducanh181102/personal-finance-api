package com.example.personalfinance.dto.debtpayment;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateDebtPaymentRequest {
    @NotNull(message = "ID khoản nợ không được để trống.")
    @Positive(message = "ID khoản nợ phải lớn hơn 0.")
    private final Integer debtId;

    @NotNull(message = "ID tài khoản không được để trống.")
    @Positive(message = "ID tài khoản phải lớn hơn 0.")
    private final Integer accountId;

    @NotNull(message = "Số tiền thanh toán không được để trống.")
    @Positive(message = "Số tiền thanh toán phải lớn hơn 0.")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.")
    private final BigDecimal amount;

    @NotNull(message = "Thời gian thanh toán không được để trống.")
    @PastOrPresent(message = "Thời gian thanh toán không được ở tương lai.")
    private final LocalDateTime paidAt;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự.")
    private final String note;

    public CreateDebtPaymentRequest(Integer debtId,
                                    Integer accountId,
                                    BigDecimal amount,
                                    LocalDateTime paidAt,
                                    String note) {
        this.debtId = debtId;
        this.accountId = accountId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.note = note;
    }

    public Integer getDebtId() {
        return debtId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getNote() {
        return note;
    }
}
