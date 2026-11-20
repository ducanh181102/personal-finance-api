package com.example.personalfinance.dto.user;

import java.time.Instant;

public class UserResponse {
    private final int id;
    private final String name;
    private final String email;
    private final Instant emailVerifiedAt;

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public UserResponse(int id, String name, String email, Instant emailVerifiedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

}
