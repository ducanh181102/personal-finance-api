package com.example.personalfinance.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Component
public class BrowserSessionCookie {

    private final boolean secure;
    private final String name;
    private final Duration lifetime;

    public BrowserSessionCookie(@Value("${app.auth.secure-cookies:false}") boolean secure,
                                @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secure = secure;
        this.name = secure ? "__Host-finance-session" : "finance-session";
        this.lifetime = Duration.ofMillis(expirationMs);
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, name);
        return cookie == null ? null : cookie.getValue();
    }

    public String issue(String token) {
        return cookie(token, lifetime);
    }

    public String clear() {
        return cookie("", Duration.ZERO);
    }

    private String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
