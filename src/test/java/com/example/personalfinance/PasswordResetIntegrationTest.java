package com.example.personalfinance;

import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.UserRepository;
import com.example.personalfinance.security.JwtService;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password-reset-integration",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.password-reset.from=no-reply@example.test",
        "app.password-reset.frontend-url=https://finance.example.test"
})
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {
    private static final String OLD_PASSWORD = "original-test-password";
    private static final String NEW_PASSWORD = "replacement-test-password";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtService jwt;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private JavaMailSender mail;

    @BeforeEach
    void configureMailMessage() {
        when(mail.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void resetsOnlyOnceAndRevokesExistingSession() throws Exception {
        String email = "password-reset-owner@example.test";
        User user = createUser(email);
        String oldJwt = jwt.generateToken(user);
        requestLink("missing-user@example.test");
        String token = requestLinkAndReadEmail(email);

        reset(token, "ộ".repeat(25)).andExpect(status().isBadRequest());
        reset(token, NEW_PASSWORD).andExpect(status().isNoContent());
        reset(token, OLD_PASSWORD).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + oldJwt))
                .andExpect(status().isUnauthorized());
        login(email, OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void rejectsExpiredLinks() throws Exception {
        String email = "password-reset-expiry@example.test";
        User user = createUser(email);
        String token = requestLinkAndReadEmail(email);
        jdbc.update("update users set password_reset_expires_at = ? where id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), user.getId());

        reset(token, NEW_PASSWORD).andExpect(status().isBadRequest());
        login(email, OLD_PASSWORD).andExpect(status().isOk());
    }

    private User createUser(String email) {
        return users.save(new User("Password Reset Test", email, encoder.encode(OLD_PASSWORD)));
    }

    private void requestLink(String email) throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    private String requestLinkAndReadEmail(String email) throws Exception {
        requestLink(email);
        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mail, timeout(5000)).send(message.capture());
        MimeMessage sent = message.getValue();
        sent.saveChanges();
        assertEquals(email, ((InternetAddress) sent.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress());
        assertEquals("Tài chính cá nhân", ((InternetAddress) sent.getFrom()[0]).getPersonal());
        String text = findBody(sent, "text/plain");
        String html = findBody(sent, "text/html");
        assertNotNull(text);
        assertNotNull(html);
        var link = Pattern.compile("https://finance\\.example\\.test/reset-password#token=([A-Za-z0-9_-]{43})")
                .matcher(text);
        assertTrue(link.find());
        assertTrue(html.contains("href=\"" + link.group() + "\""));
        assertTrue(html.contains("Đặt mật khẩu mới"));
        assertTrue(html.contains("15 phút"));
        assertFalse(html.contains("{{"));
        return link.group(1);
    }

    private String findBody(Part part, String type) throws Exception {
        if (part.isMimeType(type)) return (String) part.getContent();
        if (part.isMimeType("multipart/*")) {
            Multipart parts = (Multipart) part.getContent();
            for (int index = 0; index < parts.getCount(); index++) {
                String body = findBody(parts.getBodyPart(index), type);
                if (body != null) return body;
            }
        }
        return null;
    }

    private ResultActions reset(String token, String password) throws Exception {
        return mvc.perform(post("/api/auth/reset-password").contentType(APPLICATION_JSON)
                .content("""
                        {"token":"%s","password":"%s"}
                        """.formatted(token, password)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }
}
