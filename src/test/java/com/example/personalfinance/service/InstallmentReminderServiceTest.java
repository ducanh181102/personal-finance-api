package com.example.personalfinance.service;

import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.DebtRepository;
import com.example.personalfinance.repository.UserRepository;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:installment-reminders",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.password-reset.from=no-reply@example.test",
        "app.password-reset.frontend-url=https://finance.example.test"
})
class InstallmentReminderServiceTest {
    @Autowired private InstallmentReminderService reminders;
    @Autowired private DebtRepository debts;
    @Autowired private UserRepository users;
    @MockitoBean private JavaMailSender mail;

    @BeforeEach
    void configureMailMessage() {
        when(mail.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsOncePerUnpaidInstallmentAndRemindsAgainForTheNextPeriod() throws Exception {
        LocalDate dueDate = LocalDate.of(2030, 1, 15);
        Debt debt = createInstallment("reminders@example.test", dueDate, true);

        reminders.sendDueReminders(dueDate.minusDays(4));
        verifyNoInteractions(mail);

        reminders.sendDueReminders(dueDate.minusDays(3));
        reminders.sendDueReminders(dueDate.minusDays(2));
        debt.applyPayment(BigDecimal.valueOf(400));
        debts.save(debt);
        reminders.sendDueReminders(dueDate);

        debt.applyPayment(BigDecimal.valueOf(600));
        debts.save(debt);
        reminders.sendDueReminders(dueDate.plusMonths(1).minusDays(2));

        var sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mail, times(2)).send(sent.capture());

        MimeMessage first = sent.getAllValues().getFirst();
        first.saveChanges();
        InternetAddress from = (InternetAddress) first.getFrom()[0];
        assertEquals("no-reply@example.test", from.getAddress());
        assertEquals("Tài chính cá nhân", from.getPersonal());
        assertEquals("reminders@example.test", first.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("Nhắc lịch trả góp | Tài chính cá nhân", first.getSubject());

        String text = findBody(first, "text/plain");
        String html = findBody(first, "text/html");
        String url = "https://finance.example.test/debts/" + debt.getId();
        assertNotNull(text);
        assertNotNull(html);
        assertTrue(text.contains("15/01/2030"));
        assertTrue(text.contains(url));
        assertTrue(html.contains("15/01/2030"));
        assertTrue(html.contains("href=\"" + url + "\""));
        assertTrue(html.contains("Xem chi tiết khoản nợ"));
        assertFalse(html.contains("{{"));

        MimeMessage next = sent.getAllValues().getLast();
        next.saveChanges();
        String nextHtml = findBody(next, "text/html");
        assertNotNull(nextHtml);
        assertTrue(nextHtml.contains("15/02/2030"));
    }

    @Test
    void retriesFailureAndSkipsDisabledPaidOrOverdueInstallments() throws Exception {
        LocalDate dueDate = LocalDate.of(2030, 4, 15);
        createInstallment("retry@example.test", dueDate, true);
        createInstallment("disabled@example.test", dueDate, false);
        createInstallment("overdue@example.test", dueDate.minusDays(2), true);
        Debt paid = createInstallment("paid@example.test", dueDate, true);
        paid.applyPayment(paid.getRemainingAmount());
        debts.save(paid);
        doThrow(new MailSendException("SMTP unavailable")).doNothing().when(mail).send(any(MimeMessage.class));

        reminders.sendDueReminders(dueDate);
        reminders.sendDueReminders(dueDate);
        reminders.sendDueReminders(dueDate);

        var sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mail, times(2)).send(sent.capture());
        for (MimeMessage message : sent.getAllValues()) {
            assertEquals("retry@example.test", message.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        }
    }

    @Test
    void sendsReminderOnlyAfterEmailIsVerified() throws Exception {
        LocalDate dueDate = LocalDate.of(2031, 6, 15);
        LocalDate reminderDate = dueDate.minusDays(1);
        createInstallment("verified@example.test", dueDate, true);
        User pendingOwner = new User("Pending verification", "unverified@example.test", "password-hash");
        createInstallment(pendingOwner, dueDate, true);

        reminders.sendDueReminders(reminderDate);
        verify(mail).send(any(MimeMessage.class));

        pendingOwner.markEmailVerified(Instant.now());
        users.save(pendingOwner);
        reminders.sendDueReminders(reminderDate);

        var sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mail, times(2)).send(sent.capture());
        assertEquals("verified@example.test",
                sent.getAllValues().getFirst().getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("unverified@example.test",
                sent.getAllValues().getLast().getRecipients(MimeMessage.RecipientType.TO)[0].toString());
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

    private Debt createInstallment(String email, LocalDate dueDate, boolean enabled) {
        User owner = new User("Reminder test", email, "password-hash");
        owner.markEmailVerified(Instant.now());
        return createInstallment(owner, dueDate, enabled);
    }

    private Debt createInstallment(User owner, LocalDate dueDate, boolean enabled) {
        User savedOwner = users.save(owner);
        Debt debt = new Debt(savedOwner, "Khoản trả góp", DebtType.BORROWED, BigDecimal.valueOf(2500), dueDate,
                null, BigDecimal.valueOf(1000), dueDate);
        debt.setInstallmentEmailReminderEnabled(enabled);
        return debts.save(debt);
    }
}
