package com.example.personalfinance.service;

import com.example.personalfinance.dto.budget.BudgetSummaryResponse;
import com.example.personalfinance.dto.debt.DebtAlertResponse;
import com.example.personalfinance.dto.notification.NotificationListResponse;
import com.example.personalfinance.dto.notification.NotificationResponse;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.AppNotification;
import com.example.personalfinance.model.NotificationType;
import com.example.personalfinance.model.RecurringTransaction;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AppNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final AppNotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final BudgetService budgetService;
    private final DebtService debtService;

    public NotificationService(AppNotificationRepository notificationRepository,
                               CurrentUserService currentUserService,
                               BudgetService budgetService,
                               DebtService debtService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.budgetService = budgetService;
        this.debtService = debtService;
    }

    /** Đồng bộ các cảnh báo đang còn hiệu lực trước khi trả danh sách cho người dùng. */
    @Transactional
    public NotificationListResponse getCurrentUserNotifications() {
        User user = currentUserService.getCurrentUser();
        syncBudgetAlerts(user);
        syncDebtAlerts(user);

        List<NotificationResponse> notifications = notificationRepository
                .findTop100ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return new NotificationListResponse(
                notifications,
                notificationRepository.countByUserIdAndReadAtIsNull(user.getId())
        );
    }

    @Transactional
    public NotificationResponse markCurrentUserNotificationRead(int notificationId) {
        User user = currentUserService.getCurrentUser();
        AppNotification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new NotFoundException(notificationId));
        notification.markRead(Instant.now());
        return toResponse(notification);
    }

    @Transactional
    public void markAllCurrentUserNotificationsRead() {
        User user = currentUserService.getCurrentUser();
        Instant readAt = Instant.now();
        notificationRepository.findByUserIdAndReadAtIsNull(user.getId())
                .forEach(notification -> notification.markRead(readAt));
    }

    /** Được scheduler gọi ngay sau khi tạo thành công một giao dịch định kỳ. */
    @Transactional
    public void recordRecurringTransactionPosted(RecurringTransaction recurringTransaction, Transaction transaction) {
        LocalDate scheduledDate = transaction.getTransactionDate().toLocalDate();
        String categoryName = recurringTransaction.getCategory().getName();
        String body = transaction.getNote() == null || transaction.getNote().isBlank()
                ? "%s · %s".formatted(categoryName, scheduledDate)
                : "%s · %s".formatted(transaction.getNote(), scheduledDate);
        createIfAbsent(
                recurringTransaction.getAccount().getUser(),
                NotificationType.RECURRING_TRANSACTION_POSTED,
                "Đã ghi nhận giao dịch định kỳ",
                body,
                "/transactions/%d".formatted(transaction.getId()),
                "recurring:%d:%s".formatted(recurringTransaction.getId(), scheduledDate)
        );
    }

    private void syncBudgetAlerts(User user) {
        LocalDate month = LocalDate.now().withDayOfMonth(1);
        for (BudgetSummaryResponse alert : budgetService.getBudgetAlerts(month)) {
            boolean exceeded = alert.getStatus().name().equals("EXCEEDED");
            NotificationType type = exceeded ? NotificationType.BUDGET_EXCEEDED : NotificationType.BUDGET_WARNING;
            String title = exceeded ? "Đã vượt ngân sách" : "Ngân sách sắp chạm hạn mức";
            String percentage = alert.getUsagePercentage().setScale(0, RoundingMode.HALF_UP).toPlainString();
            String body = "%s đã dùng %s%% ngân sách tháng %s."
                    .formatted(alert.getCategoryName(), percentage, MONTH_FORMAT.format(month));
            createIfAbsent(
                    user,
                    type,
                    title,
                    body,
                    "/budgets/%d".formatted(alert.getBudgetId()),
                    "budget:%d:%s:%s".formatted(alert.getBudgetId(), month, type)
            );
        }
    }

    private void syncDebtAlerts(User user) {
        for (DebtAlertResponse alert : debtService.getDebtAlerts(7)) {
            NotificationType type = alert.isOverdue() ? NotificationType.DEBT_OVERDUE : NotificationType.DEBT_DUE;
            String subject = alert.isInstallment() ? "Kỳ trả góp" : "Khoản công nợ";
            String title = alert.isOverdue() ? subject + " đã quá hạn" : subject + " sắp đến hạn";
            String body = "%s · hạn %s".formatted(alert.getPersonName(), alert.getDueDate());
            createIfAbsent(
                    user,
                    type,
                    title,
                    body,
                    "/debts/%d".formatted(alert.getDebtId()),
                    "debt:%d:%s:%s".formatted(alert.getDebtId(), alert.getDueDate(), type)
            );
        }
    }

    private void createIfAbsent(User user, NotificationType type, String title, String body,
                                String targetPath, String deduplicationKey) {
        if (notificationRepository.findByUserIdAndDeduplicationKey(user.getId(), deduplicationKey).isPresent()) return;
        notificationRepository.save(new AppNotification(
                user, type, title, body, targetPath, deduplicationKey, Instant.now()
        ));
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetPath(),
                notification.getCreatedAt(),
                notification.getReadAt() != null
        );
    }
}
