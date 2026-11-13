package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.user.UserResponse;
import com.example.personalfinance.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
