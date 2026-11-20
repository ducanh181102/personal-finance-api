package com.example.personalfinance.security;

import com.example.personalfinance.model.User;
import com.example.personalfinance.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserService userService;
    private final BrowserSessionCookie sessionCookie;

    public JwtAuthenticationFilter(JwtService jwtService, UserService userService, BrowserSessionCookie sessionCookie) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.sessionCookie = sessionCookie;
    }

    public static boolean usesBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.startsWith(BEARER_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Bearer được chọn trước cookie để request API dùng đúng phiên mà client chỉ định.
        String token = usesBearerToken(request)
                ? request.getHeader(HttpHeaders.AUTHORIZATION).substring(BEARER_PREFIX.length())
                : sessionCookie.read(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(token);
            Optional<User> user = userService.getUserByEmail(claims.getSubject());

            if (user.isPresent() && user.get().isActive()
                    && Objects.equals(user.get().getSessionVersion(), claims.get(JwtService.SESSION_VERSION_CLAIM, String.class))) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.get(), // principal
                                null,
                                List.of()
                        );
                // Sau này ta có thể lấy user hiện tại từ đây, thay vì tin userId client gửi lên.
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
