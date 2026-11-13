package com.example.personalfinance.dto.debt;

import com.example.personalfinance.model.DebtType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateDebtRequest {
    @NotBlank(message = "Tên người liên quan không được để trống.")
    @Size(max = 100, message = "Tên người liên quan tối đa 100 ký tự.")
    private final String personName;

    @NotNull(message = "Loại khoản nợ không được để trống.")
    private final DebtType type;

    @NotNull(message = "Số tiền gốc không được để trống.")
    @Positive(message = "Số tiền gốc phải lớn hơn 0.")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.")
    private final BigDecimal originalAmount;

    @NotNull(message = "Hạn thanh toán không được để trống.")
    @FutureOrPresent(message = "Hạn thanh toán không được ở quá khứ.")
    private final LocalDate dueDate;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự.")
    private final String note;

    public CreateDebtRequest(String personName,
                             DebtType type,
                             BigDecimal originalAmount,
                             LocalDate dueDate,
                             String note) {
        this.personName = personName;
        this.type = type;
        this.originalAmount = originalAmount;
        this.dueDate = dueDate;
        this.note = note;
    }

    public String getPersonName() {
        return personName;
    }

    public DebtType getType() {
        return type;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getNote() {
        return note;
    }
}
