package com.example.personalfinance.controller;

import com.example.personalfinance.dto.LoginRequest;
import com.example.personalfinance.dto.LoginResponse;
import com.example.personalfinance.dto.ReactivateRequest;
import com.example.personalfinance.dto.ReactivateResponse;
import com.example.personalfinance.mapper.UserMapper;
import com.example.personalfinance.model.User;
import com.example.personalfinance.security.JwtService;
import com.example.personalfinance.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public AuthController(AuthService authService, UserMapper userMapper, JwtService jwtService) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.getAuthentication(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, userMapper.toUserResponse(user));
    }

    @PostMapping("/reactivate")
    public ReactivateResponse reactivate(@Valid @RequestBody ReactivateRequest request) {
        User user = authService.reactivate(request.getEmail(), request.getOldPassword());
        String newToken = jwtService.generateToken(user);
        return new ReactivateResponse(newToken, userMapper.toUserResponse(user));
    }
}
