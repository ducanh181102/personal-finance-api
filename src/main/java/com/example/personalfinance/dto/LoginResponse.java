package com.example.personalfinance.dto;

public class LoginResponse {
    private final String accessToken;
    private final UserResponse userResponse;

    public LoginResponse(String accessToken, UserResponse userResponse) {
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
