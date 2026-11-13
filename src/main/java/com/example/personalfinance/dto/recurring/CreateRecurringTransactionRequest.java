package com.example.personalfinance.dto.recurring;

import com.example.personalfinance.model.RecurrenceFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateRecurringTransactionRequest {
    @NotNull(message = "ID tài khoản không được phép trống.")
    @Positive(message = "ID tài khoản phải lớn hơn 0.")
    private final Integer accountId;
    @Positive(message = "ID danh mục phải lớn hơn 0.")
    @NotNull(message = "ID danh mục không được phép trống.")
    private final Integer categoryId;
    @NotNull(message = "Số tiền không được phép trống.")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.")
    @Positive(message = "Số tiền giao dịch phải lớn hơn 0.")
    private final BigDecimal amount;
    @Size(max = 255, message = "Ghi chú tối đa 255 kí tự.")
    private final String note;
    @NotNull(message = "Kế hoạch định kỳ không được phép trống.")
    private final RecurrenceFrequency frequency;
    @NotNull(message = "Ngày thực thi kế tiếp không được phép trống.")
    @FutureOrPresent(message = "Ngày thực thi kế tiếp không được ở quá khứ.")
    private final LocalDate nextRunDate;

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

    public CreateRecurringTransactionRequest(Integer accountId, Integer categoryId, BigDecimal amount, String note, RecurrenceFrequency frequency, LocalDate nextRunDate) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.note = note;
        this.frequency = frequency;
        this.nextRunDate = nextRunDate;
    }
}
