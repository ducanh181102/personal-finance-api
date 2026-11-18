package com.example.personalfinance.service;

import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final Duration RESEND_DELAY = Duration.ofMinutes(1);
    private static final String INVALID_LINK = "Liên kết không hợp lệ hoặc đã hết hạn.";
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor executor;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String frontendUrl;
    private final String emailTemplate;

    public PasswordResetService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            @Qualifier("passwordResetExecutor") TaskExecutor executor,
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.password-reset.from:}") String from,
            @Value("${app.password-reset.frontend-url}") String frontendUrl
    ) throws IOException {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.executor = executor;
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
        // Đọc mẫu HTML một lần khi khởi động để dùng cho các email khôi phục.
        this.emailTemplate = new ClassPathResource("templates/mail/password-reset.html")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    public boolean requestReset(String email) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank()) {
            // Ghi trạng thái cấu hình để kiểm tra lỗi 503, chỉ hiển thị các giá trị boolean.
            log.warn("Cấu hình gửi mail chưa đầy đủ (JavaMailSender có sẵn: {}, địa chỉ người gửi đã đặt: {}). "
                            + "Kiểm tra SPRING_MAIL_HOST và PASSWORD_RESET_FROM hoặc SPRING_MAIL_USERNAME.",
                    sender != null, !from.isBlank());
            return false;
        }

        // Tra tài khoản và gửi mail trong hàng đợi để phản hồi giống nhau cho mọi email.
        try {
            executor.execute(() -> sendResetEmail(email.trim(), sender));
            return true;
        } catch (TaskRejectedException exception) {
            return false;
        }
    }

    // Nhận địa chỉ người nhận và bộ gửi thư, thực hiện tác vụ trên luồng nền của executor.
    private void sendResetEmail(String email, JavaMailSender sender) {
        // Bắt lỗi xảy ra trong quá trình cấp token, tạo thư hoặc gửi thư.
        try {
            // Chạy issueToken trong transaction, commit dữ liệu rồi nhận token gốc để đưa vào email.
            // status là đối tượng quản lý transaction được Spring truyền vào lambda.
            String token = transactionTemplate.execute(status -> issueToken(email));
            // Dừng khi tài khoản không đủ điều kiện hoặc chưa hết thời gian chờ gửi lại.
            if (token == null) return;

            // Bắt đầu tạo liên kết từ địa chỉ frontend đã cấu hình.
            String link = UriComponentsBuilder.fromUriString(frontendUrl)
                    // Thêm đường dẫn đến trang nhập mật khẩu mới.
                    .pathSegment("reset-password")
                    // Thêm #token=... để React đọc token khi người dùng mở liên kết.
                    .fragment("token=" + token)
                    // Hoàn tất việc tạo URI và chuyển thành chuỗi để đưa vào nội dung thư.
                    .build().toUriString();
            // Tạo email MIME để gửi cả nội dung văn bản và giao diện HTML bằng UTF-8.
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            // Đặt tên hiển thị và địa chỉ gửi để người nhận nhận diện ứng dụng.
            helper.setFrom(from, "Tài chính cá nhân");
            // Đặt địa chỉ nhận thư là email được yêu cầu khôi phục.
            helper.setTo(email);
            // Đặt tiêu đề hiển thị trong hộp thư của người nhận.
            helper.setSubject("Đặt lại mật khẩu Tài chính cá nhân");
            // Cung cấp nội dung tương đương cho ứng dụng đọc mail ở chế độ văn bản.
            String text = """
                    Tài chính cá nhân

                    Xin chào,

                    Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                    Mở liên kết dưới đây để tạo mật khẩu mới.

                    %s

                    Liên kết có hiệu lực trong %d phút và chỉ dùng được một lần.

                    Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                    """.formatted(link, TOKEN_LIFETIME.toMinutes());
            // Điền cùng một liên kết vào nút và đường dẫn dự phòng, escape để dùng an toàn trong HTML.
            String html = emailTemplate.replace("{{resetUrl}}", HtmlUtils.htmlEscape(link))
                    .replace("{{expiryMinutes}}", Long.toString(TOKEN_LIFETIME.toMinutes()));
            // Đặt hai phiên bản vào multipart/alternative để trình đọc thư chọn cách hiển thị phù hợp.
            helper.setText(text, html);
            // Gửi email tới máy chủ SMTP bằng JavaMailSender đã được Spring cấu hình.
            sender.send(message);
        // Ghi lại lỗi tạo MIME, mã hóa tên người gửi hoặc lỗi runtime trên luồng nền.
        } catch (MessagingException | IOException | RuntimeException exception) {
            // Thay {} bằng tên lớp lỗi, ví dụ MailSendException, để hỗ trợ kiểm tra lỗi gửi thư.
            log.warn("Không gửi được email đặt lại mật khẩu ({})", exception.getClass().getSimpleName());
        }
    }

    // Cấp token trong transaction do sendResetEmail mở bằng transactionTemplate.execute.
    private String issueToken(String email) {
        // Tìm theo email và khóa bản ghi đến cuối transaction, chỉ giữ tài khoản đang hoạt động.
        User user = users.findForUpdateByEmail(email).filter(User::isActive).orElse(null);
        // Dừng nếu email chưa đăng ký hoặc tài khoản đã bị vô hiệu hóa.
        if (user == null) return null;

        // Lấy thời điểm hiện tại để kiểm tra khoảng chờ và tính hạn của token mới.
        Instant now = Instant.now();
        // Đọc thời điểm hết hạn của token trước đó, có thể null nếu chưa có token.
        Instant expiresAt = user.getPasswordResetExpiresAt();
        // Hạn cũ trừ 15 phút là lúc cấp token, cộng 1 phút là mốc được phép cấp lại.
        // Nếu mốc này vẫn ở tương lai thì người dùng chưa chờ đủ 1 phút.
        if (expiresAt != null && expiresAt.minus(TOKEN_LIFETIME).plus(RESEND_DELAY).isAfter(now)) {
            // Dừng cấp token để giới hạn tần suất gửi email cho tài khoản này.
            return null;
        }

        // Tạo mảng chứa 32 byte, tương ứng 256 bit dữ liệu ngẫu nhiên.
        byte[] bytes = new byte[32];
        // Dùng SecureRandom điền dữ liệu ngẫu nhiên vào mảng để token khó đoán.
        random.nextBytes(bytes);
        // Mã hóa thành chuỗi Base64 dùng được trong URL, bỏ dấu đệm = để được 43 ký tự.
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // Gán bản băm SHA-256 và hạn 15 phút, thay thông tin token cũ nếu có.
        // User đang được JPA quản lý nên thay đổi được lưu khi transaction commit.
        user.issuePasswordReset(hash(token), now.plus(TOKEN_LIFETIME));
        // Trả token gốc cho sendResetEmail ghép vào liên kết đặt lại mật khẩu.
        return token;
    }

    // Chạy trong một transaction, hoàn tác thay đổi database nếu phát sinh RuntimeException.
    @Transactional
    // Nhận token từ liên kết khôi phục và mật khẩu mới do người dùng nhập.
    public void resetPassword(String token, String password) {
        // Chuyển mật khẩu thành byte UTF-8 rồi kiểm tra giới hạn 72 byte của BCrypt.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            // Báo lỗi khi vượt giới hạn, GlobalExceptionHandler chuyển lỗi này thành HTTP 400.
            throw new IllegalArgumentException("Mật khẩu tối đa 72 byte UTF-8.");
        }
        // Băm token nhận được để tìm tài khoản có bản băm tương ứng, chỉ giữ tài khoản đang hoạt động.
        // Khóa bản ghi đến cuối transaction để các request đồng thời lần lượt kiểm tra và sử dụng token.
        User user = users.findForUpdateByPasswordResetHash(hash(token)).filter(User::isActive)
                // Báo liên kết không hợp lệ khi token không khớp hoặc tài khoản đã bị vô hiệu hóa.
                .orElseThrow(() -> new IllegalArgumentException(INVALID_LINK));
        // Token còn hiệu lực khi có hạn sử dụng và hạn đó nằm sau thời điểm hiện tại.
        // Thiếu hạn hoặc hạn đã đến thì từ chối yêu cầu, kể cả đúng thời điểm hết hạn.
        if (user.getPasswordResetExpiresAt() == null || !user.getPasswordResetExpiresAt().isAfter(Instant.now())) {
            // Dừng đặt lại mật khẩu khi liên kết không còn hiệu lực.
            throw new IllegalArgumentException(INVALID_LINK);
        }
        // Băm mật khẩu mới bằng PasswordEncoder rồi cập nhật User đang được JPA quản lý.
        // setPasswordHash đồng thời đổi sessionVersion và xóa token cùng thời hạn của token.
        // Khi transaction commit, JPA lưu thay đổi, liên kết đã dùng và các JWT cũ mất hiệu lực.
        user.setPasswordHash(passwordEncoder.encode(password));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
