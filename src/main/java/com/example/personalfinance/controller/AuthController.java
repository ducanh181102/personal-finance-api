package com.example.personalfinance.controller;

import com.example.personalfinance.dto.auth.*;
import com.example.personalfinance.dto.user.UserResponse;
import com.example.personalfinance.mapper.UserMapper;
import com.example.personalfinance.model.User;
import com.example.personalfinance.security.BrowserSessionCookie;
import com.example.personalfinance.security.JwtService;
import com.example.personalfinance.service.AuthService;
import com.example.personalfinance.service.EmailVerifyService;
import com.example.personalfinance.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final BrowserSessionCookie sessionCookie;
    private final EmailVerifyService emailVerifyService;

    public AuthController(AuthService authService, UserMapper userMapper, JwtService jwtService,
                          PasswordResetService passwordResetService, BrowserSessionCookie sessionCookie, EmailVerifyService emailVerifyService) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.sessionCookie = sessionCookie;
        this.emailVerifyService = emailVerifyService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.getAuthentication(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, userMapper.toUserResponse(user));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        // Spring tạo và gửi cookie CSRF khi token được đọc lần đầu.
        token.getToken();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/session")
    public ResponseEntity<UserResponse> startSession(@Valid @RequestBody LoginRequest request) {
        User user = authService.getAuthentication(request.getEmail(), request.getPassword());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, sessionCookie.issue(jwtService.generateToken(user)))
                .body(userMapper.toUserResponse(user));
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> endSession() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sessionCookie.clear()).build();
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
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return endSession();
    }

    @PostMapping("/request-verify-email")
    public ResponseEntity<Void> requestVerifyEmail(@Valid @RequestBody RequestVerifyEmailRequest request) {
        emailVerifyService.requestVerify(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerifyService.verifyEmail(request.token());
        return ResponseEntity.accepted().build();
    }
}
