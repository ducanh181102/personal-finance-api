package com.example.personalfinance.dto;

public class ReactivateResponse {
    private final String accessToken;
    private final UserResponse userResponse;

    public ReactivateResponse(String accessToken, UserResponse userResponse) {
        this.accessToken = accessToken;
        this.userResponse = userResponse;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public UserResponse getUserResponse() {
        return userResponse;
    }
}
