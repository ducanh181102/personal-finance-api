package com.example.personalfinance.dto;

import java.math.BigDecimal;

public class AccountBalanceResponse {
    private final Integer accountId;
    private final BigDecimal currentBalance;

    public AccountBalanceResponse(Integer accountId, BigDecimal currentBalance) {
        this.accountId = accountId;
        this.currentBalance = currentBalance;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
}
