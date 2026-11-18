package com.example.personalfinance.model;

import java.time.LocalDate;

public enum RecurrenceFrequency {
    MONTHLY;

    public LocalDate nextDateAfter(LocalDate date) {
        return switch (this) {
            case MONTHLY -> date.plusMonths(1);
        };
    }
}
