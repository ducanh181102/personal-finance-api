package com.example.personalfinance.config;

import com.example.personalfinance.security.BrowserSessionCookie;
import com.example.personalfinance.security.JwtAuthenticationFilter;
import com.example.personalfinance.security.JwtService;
import com.example.personalfinance.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

// @Configuration nói với Spring: đây là class cấu hình, không phải controller hay service.
// Khi ứng dụng khởi động, Spring đọc class này để biết:
//- API nào public.
//- API nào phải đăng nhập.
//- Filter JWT nằm ở đâu.
//- Không dùng session.
//- Khi chưa đăng nhập thì trả status nào.
@Configuration
public class SecurityConfig {

    // SecurityConfig cần hai object này để tạo filter JWT:
    //- JwtService: kiểm tra token và lấy email.
    //- UserService: dùng email tìm user trong database.
    private final JwtService jwtService;
    private final UserService userService;

    public SecurityConfig(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userService.getUserByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .disabled(!user.isActive())
                        .roles("USER")
                        .build()
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException("Không tìm thấy user: " + email)
                );
    }

    // @Bean bảo Spring: hãy gọi method này lúc khởi động, lấy object trả về và dùng nó làm cấu hình bảo mật.
    // SecurityFilterChain là chuỗi các filter xử lý request trước controller.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    BrowserSessionCookie sessionCookie,
                                                    @Value("${app.auth.secure-cookies:false}") boolean secureCookies)
            throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, userService, sessionCookie);

        RequestMatcher browserSession = PathPatternRequestMatcher.withDefaults().matcher("/api/auth/session");
        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokens.setCookieName(secureCookies ? "__Host-finance-csrf" : "finance-csrf");
        csrfTokens.setCookieCustomizer(cookie -> cookie.path("/").secure(secureCookies).sameSite("Lax"));

        // là object Spring đưa vào để ta cấu hình bảo mật theo kiểu “builder”.
        return http
                // React đọc cookie CSRF và gửi giá trị qua header X-XSRF-TOKEN.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(request -> !browserSession.matches(request)
                                && (JwtAuthenticationFilter.usesBearerToken(request)
                                    || sessionCookie.read(request) == null))
                )

                // server không nhớ trạng thái đăng nhập giữa các request
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // tắt vì Spring có thể chuyển client sang trang login HTML mặc định.
                // formLogin → HTML login mặc định + session/cookie
                .formLogin(AbstractHttpConfigurer::disable)
                // httpBasic → gửi username/password trong header mỗi request
                // tắt Vì Spring có thể chờ username/password theo kiểu Basic Auth
                .httpBasic(AbstractHttpConfigurer::disable)
                // vì dự án đang dùng jwt, JWT       → login nhận token, request sau gửi Bearer token
                // nên tắt 2 cái trên

                // Khi chưa đăng nhập: trả 401
                // Đoạn này quy định: nếu endpoint yêu cầu đăng nhập nhưng request không có authentication hợp lệ, trả:
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                        // Trả 403 trực tiếp để servlet giữ đúng trạng thái khi CSRF bị từ chối.
                        .accessDeniedHandler((request, response, denied) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value()))
                )

                .authorizeHttpRequests(auth -> auth
                                // URL bên dưới được gọi mà không cần token.
                                .requestMatchers(
                                        "/api/auth/login",
                                        "/api/auth/reactivate",
                                        "/api/auth/session",
                                        "/api/auth/csrf"
                                ).permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/request-verify-email", "/api/auth/verify-email").permitAll()
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/users"
                                ).permitAll()

                                // Mọi request không khớp rule permitAll() phía trên đều phải có user trong SecurityContext.
                                // Nơi kiểm tra có cho vào controller ko
                                .anyRequest().authenticated()

                        // Phải để endpoint public ở trên. Nếu viết anyRequest() trước, nó sẽ bắt mọi request trước khi Spring kịp thấy rule login/register.
                )

                // Gắn filter JWT vào đúng vị trí
                .addFilterBefore(
                        // Đặt JwtAuthenticationFilter chạy trước UsernamePasswordAuthenticationFilter của Spring Security
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }
}
