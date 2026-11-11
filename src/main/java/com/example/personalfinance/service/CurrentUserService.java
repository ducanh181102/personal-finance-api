package com.example.personalfinance.service;

import com.example.personalfinance.exception.InvalidCredentialsException;
import com.example.personalfinance.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user))
            throw new InvalidCredentialsException("Không thể xác thực người dùng.");
        return (User) authentication.getPrincipal();
    }
}
