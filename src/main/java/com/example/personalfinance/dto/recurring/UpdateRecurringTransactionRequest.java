package com.example.personalfinance.dto.recurring;

import com.example.personalfinance.model.RecurrenceFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateRecurringTransactionRequest {
    @Positive(message = "ID tài khoản phải lớn hơn 0.")
    private final Integer accountId;
    @Positive(message = "ID danh mục phải lớn hơn 0.")
    private final Integer categoryId;
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.")
    @Positive(message = "Số tiền giao dịch phải lớn hơn 0.")
    private final BigDecimal amount;
    @Size(max = 255, message = "Ghi chú tối đa 255 kí tự.")
    private final String note;
    private final RecurrenceFrequency frequency;
    @FutureOrPresent(message = "Ngày thực thi kế tiếp không được ở quá khứ.")
    private final LocalDate nextRunDate;
    private final Boolean active;

    public Boolean isActive() {
        return active;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public UpdateRecurringTransactionRequest(Integer accountId, Integer categoryId, BigDecimal amount, String note, RecurrenceFrequency frequency, LocalDate nextRunDate, Boolean active) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.note = note;
        this.frequency = frequency;
        this.nextRunDate = nextRunDate;
        this.active = active;
    }
}
