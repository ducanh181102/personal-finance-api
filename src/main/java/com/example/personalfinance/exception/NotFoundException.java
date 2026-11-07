package com.example.personalfinance.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(int id) {
        super("Không tìm thấy đối tượng có ID: " + id);
    }
}

