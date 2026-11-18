package com.example.personalfinance.dto.debt;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateDebtRequest {
    @Pattern(regexp = ".*\\S.*", message = "Tên người liên quan không được chỉ chứa khoảng trắng.")
    @Size(max = 100, message = "Tên người liên quan tối đa 100 ký tự.")
    private final String personName;

    @FutureOrPresent(message = "Hạn thanh toán không được ở quá khứ.")
    private final LocalDate dueDate;

    private final Boolean installmentEnabled;

    @Positive(message = "Số tiền trả góp mỗi tháng phải lớn hơn 0.")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.")
    private final java.math.BigDecimal monthlyInstallmentAmount;

    @FutureOrPresent(message = "Ngày trả kỳ đầu không được ở quá khứ.")
    private final LocalDate firstInstallmentDate;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự.")
    private final String note;

    public UpdateDebtRequest(String personName,
                             LocalDate dueDate,
                             Boolean installmentEnabled,
                             java.math.BigDecimal monthlyInstallmentAmount,
                             LocalDate firstInstallmentDate,
                             String note) {
        this.personName = personName;
        this.dueDate = dueDate;
        this.installmentEnabled = installmentEnabled;
        this.monthlyInstallmentAmount = monthlyInstallmentAmount;
        this.firstInstallmentDate = firstInstallmentDate;
        this.note = note;
    }

    public String getPersonName() {
        return personName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Boolean getInstallmentEnabled() {
        return installmentEnabled;
    }

    public java.math.BigDecimal getMonthlyInstallmentAmount() {
        return monthlyInstallmentAmount;
    }

    public LocalDate getFirstInstallmentDate() {
        return firstInstallmentDate;
    }

    public String getNote() {
        return note;
    }
}
