package com.example.personalfinance.controller;

import com.example.personalfinance.dto.auth.LoginRequest;
import com.example.personalfinance.dto.auth.ForgotPasswordRequest;
import com.example.personalfinance.dto.auth.LoginResponse;
import com.example.personalfinance.dto.auth.ReactivateRequest;
import com.example.personalfinance.dto.auth.ReactivateResponse;
import com.example.personalfinance.dto.auth.ResetPasswordRequest;
import com.example.personalfinance.mapper.UserMapper;
import com.example.personalfinance.model.User;
import com.example.personalfinance.security.JwtService;
import com.example.personalfinance.service.AuthService;
import com.example.personalfinance.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, UserMapper userMapper, JwtService jwtService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestReset(request.email())
                ? ResponseEntity.accepted().build()
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
    }
}
