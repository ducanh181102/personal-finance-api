package com.example.personalfinance.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AccountType {
    CASH,
    BANK,
    E_WALLET,
    OTHER;

    // Đọc mã theo tên để Jackson không chuyển số thứ tự thành loại ví.
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AccountType fromCode(String code) {
        return AccountType.valueOf(code);
    }
}
