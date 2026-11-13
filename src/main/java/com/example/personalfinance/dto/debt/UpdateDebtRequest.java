package com.example.personalfinance.dto.debt;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateDebtRequest {
    @Pattern(regexp = ".*\\S.*", message = "Tên người liên quan không được chỉ chứa khoảng trắng.")
    @Size(max = 100, message = "Tên người liên quan tối đa 100 ký tự.")
    private final String personName;

    @FutureOrPresent(message = "Hạn thanh toán không được ở quá khứ.")
    private final LocalDate dueDate;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự.")
    private final String note;

    public UpdateDebtRequest(String personName, LocalDate dueDate, String note) {
        this.personName = personName;
        this.dueDate = dueDate;
        this.note = note;
    }

    public String getPersonName() {
        return personName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getNote() {
        return note;
    }
}
