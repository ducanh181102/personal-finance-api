package com.example.personalfinance.dto.savingsgoal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateSavingsGoalRequest {
    @Pattern(regexp = ".*\\S.*", message = "Tên mục tiêu không được chỉ chứa khoảng trắng.")
    @Size(max = 100, message = "Tên mục tiêu tối đa 100 ký tự.")
    private final String name;
    @Positive(message = "Số tiền mục tiêu phải lớn hơn 0.")
    @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân.")
    private final BigDecimal targetAmount;
    @FutureOrPresent(message = "Hạn hoàn thành không được ở quá khứ.")
    private final LocalDate deadline;

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public UpdateSavingsGoalRequest(String name, BigDecimal targetAmount, LocalDate deadline) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
    }
}
