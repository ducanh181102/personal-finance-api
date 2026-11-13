package com.example.personalfinance.dto.goalcontribution;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateGoalContributionRequest {
    @NotNull(message = "ID túi tiết kiệm không được trống.")
    @Positive(message = "ID túi tiết kiệm phải lớn hơn 0.")
    private final Integer savingsGoalId;
    @NotNull(message = "ID tài khoản không được trống.")
    @Positive(message = "ID tài khoản phải lớn hơn 0.")
    private final Integer accountId;
    @NotNull(message = "Số tiền không được để trống.")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
    @Positive(message = "Số tiền phải lớn hơn 0.")
    private final BigDecimal amount;
    @PastOrPresent(message = "Thời gian gửi tiết kiệm không được ở tương lai.")
    @NotNull(message = "Thời gian gửi tiết kiệm không được trống.")
    private final LocalDateTime contributedAt;
    @Size(max = 255, message = "Ghi chú tối đa 255 kí tự.")
    private final String note;

    public CreateGoalContributionRequest(Integer savingsGoalId, Integer accountId, BigDecimal amount, LocalDateTime contributedAt, String note) {
        this.savingsGoalId = savingsGoalId;
        this.accountId = accountId;
        this.amount = amount;
        this.contributedAt = contributedAt;
        this.note = note;
    }

    public Integer getSavingsGoalId() {
        return savingsGoalId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getContributedAt() {
        return contributedAt;
    }

    public String getNote() {
        return note;
    }
}
