package com.example.personalfinance.service;

import com.example.personalfinance.exception.InvalidCredentialsException;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    private User authentication(String email, String password) {
        String msgErr = "Tài khoản hoặc mật khẩu không đúng.";
        Optional<User> user = userService.getUserByEmail(email);
        if (user.isEmpty()) throw new InvalidCredentialsException(msgErr);
        String passwordHash = user.get().getPasswordHash();
        if (!passwordEncoder.matches(password, passwordHash))
            throw new InvalidCredentialsException(msgErr);
        return user.get();
    }

    public User getAuthentication(String email, String password) {
        User user = authentication(email, password);
        if (!user.isActive()) throw new IllegalArgumentException("Tài khoản không hoạt động");
        return user;
    }

    public User reactivate(String email, String password) {
        User user = authentication(email, password);
        if (user.isActive()) throw new IllegalArgumentException("Tài khoản đang hoạt động");
        user.activate();
        return userRepository.save(user);
    }
}
