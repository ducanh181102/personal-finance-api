package com.example.personalfinance.dto;

import java.math.BigDecimal;

public class AccountResponse {
    private final int id;
    private final String name;
    private final String type;
    private final BigDecimal openingBalance;
    private final UserResponse user;

    public int getId() {
        return id;
    }

    public UserResponse getUser() {
        return user;
    }

    public AccountResponse(int id, String name, String type, BigDecimal openingBalance, UserResponse user) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.openingBalance = openingBalance;
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

}
