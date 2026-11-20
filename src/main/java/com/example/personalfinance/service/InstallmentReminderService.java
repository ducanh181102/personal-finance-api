package com.example.personalfinance.service;

import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtStatus;
import com.example.personalfinance.repository.DebtRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class InstallmentReminderService {
    private static final Logger log = LoggerFactory.getLogger(InstallmentReminderService.class);
    private static final String SCHEDULE_ZONE = "Asia/Ho_Chi_Minh";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int DAYS_BEFORE_DUE = 3;

    private final DebtRepository debts;
    private final TransactionTemplate transactions;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String frontendUrl;
    private final String emailTemplate;

    public InstallmentReminderService(DebtRepository debts,
                                      TransactionTemplate transactions,
                                      ObjectProvider<JavaMailSender> mailSender,
                                      @Value("${app.password-reset.from:}") String from,
                                      @Value("${app.password-reset.frontend-url}") String frontendUrl) throws IOException {
        this.debts = debts;
        this.transactions = transactions;
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
        // Đọc mẫu HTML một lần khi khởi động.
        this.emailTemplate = new ClassPathResource("templates/mail/installment-reminder.html")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Scheduled(cron = "0 0 * * * *", zone = SCHEDULE_ZONE)
    public void runEveryHour() {
        sendDueReminders(LocalDate.now(ZoneId.of(SCHEDULE_ZONE)));
    }

    void sendDueReminders(LocalDate today) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank() || frontendUrl.isBlank()) return;

        LocalDate cutoff = today.plusDays(DAYS_BEFORE_DUE);
        for (int debtId : debts.findInstallmentReminderCandidates(DebtStatus.OPEN, today, cutoff)) {
            try {
                // Khóa từng khoản để các lượt chạy đồng thời không cùng gửi cho một kỳ.
                transactions.executeWithoutResult(status -> debts.findForUpdateById(debtId)
                        .ifPresent(debt -> sendReminderIfDue(debt, today, cutoff, sender)));
            } catch (RuntimeException exception) {
                log.warn("Không gửi được email nhắc trả góp cho khoản #{} ({})",
                        debtId, exception.getClass().getSimpleName());
            }
        }
    }

    // Xét một khoản nợ và gửi email nếu kỳ trả góp tiếp theo nằm trong khoảng cần nhắc.
    // today là ngày kiểm tra, cutoff là ngày cuối của khoảng nhắc, sender là đối tượng gửi email.
    private void sendReminderIfDue(Debt debt, LocalDate today, LocalDate cutoff, JavaMailSender sender) {
        // Bỏ qua nếu người dùng đã tắt email nhắc nợ hoặc tài khoản đã bị vô hiệu hóa.
        if (!debt.isInstallmentEmailReminderEnabled() || !debt.getUser().isActive() || debt.getUser().getEmailVerifiedAt() == null)
            return;

        // Lấy ngày đến hạn của kỳ trả góp tiếp theo.
        LocalDate dueDate = debt.getNextInstallmentDate();

        // Chỉ nhắc các kỳ có ngày đến hạn từ today đến cutoff, tính cả hai đầu.
        if (dueDate == null                              // Chưa có kỳ trả góp tiếp theo.
                || dueDate.isBefore(today)               // Kỳ này đã quá hạn.
                || dueDate.isAfter(cutoff)                // Kỳ này còn xa, chưa tới thời điểm nhắc.
                || debt.wasInstallmentReminderSent(dueDate)) return; // Kỳ này đã được nhắc rồi.

        // Bắt đầu từ địa chỉ frontend, ví dụ https://financeducanh.id.vn.
        String link = UriComponentsBuilder.fromUriString(frontendUrl)
                // Nối đường dẫn đến chi tiết khoản nợ, ví dụ /debts/12.
                .pathSegment("debts", Integer.toString(debt.getId()))
                // Hoàn tất URL và chuyển thành chuỗi để chèn vào email.
                .build().toUriString();

        String formattedDueDate = DATE_FORMAT.format(dueDate);
        // Giữ nội dung văn bản cho trình đọc thư không hiển thị HTML.
        String text = """
                Xin chào,

                Bạn có một kỳ trả góp chưa hoàn tất, đến hạn ngày %s.
                Đăng nhập để xem số tiền còn lại của kỳ này và ghi nhận thanh toán.

                %s

                Bạn có thể tắt email nhắc lịch trong mục Sửa khoản nợ.

                Tài chính cá nhân
                """.formatted(formattedDueDate, link);
        // Escape URL trước khi điền vào thuộc tính href và phần hiển thị của mẫu HTML.
        String html = emailTemplate.replace("{{dueDate}}", formattedDueDate)
                .replace("{{debtUrl}}", HtmlUtils.htmlEscape(link));

        // Đóng gói hai phiên bản trong cùng email và mã hóa tiếng Việt bằng UTF-8.
        MimeMessage message = sender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "Tài chính cá nhân");
            helper.setTo(debt.getUser().getEmail());
            helper.setSubject("Nhắc lịch trả góp | Tài chính cá nhân");
            helper.setText(text, html);
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException(exception);
        }

        // Nếu SMTP báo lỗi, lượt chạy tiếp theo vẫn có thể gửi lại.
        sender.send(message);

        // Ghi nhận đã gửi lời nhắc cho đúng kỳ này để lượt kiểm tra sau bỏ qua.
        debt.markInstallmentReminderSent(dueDate);
    }
}
