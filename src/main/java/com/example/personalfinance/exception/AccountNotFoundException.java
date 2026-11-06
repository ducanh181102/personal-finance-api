package com.example.personalfinance.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(int id) {
        super("Không tìm thấy tài khoản có ID: " + id);
    }
}
