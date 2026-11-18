package com.example.personalfinance.service;

import com.example.personalfinance.dto.debt.DebtAlertResponse;
import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtStatus;
import com.example.personalfinance.model.DebtType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.DebtPaymentRepository;
import com.example.personalfinance.repository.DebtRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DebtService {
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;
    private final DebtPaymentRepository debtPaymentRepository;

    public DebtService(DebtRepository debtRepository, CurrentUserService currentUserService, DebtPaymentRepository debtPaymentRepository) {
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
        this.debtPaymentRepository = debtPaymentRepository;
    }

    private Debt getOwnedDebtById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Debt> debt = debtRepository.findById(id);
        if (debt.isEmpty()) throw new NotFoundException(id);
        if (debt.get().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return debt.get();
    }

    public List<Debt> getAllDebts() {
        return debtRepository.findByUserId(currentUserService.getCurrentUser().getId());
    }

    public Debt getDebtById(int id) {
        return getOwnedDebtById(id);
    }

    public Debt createDebt(String personName,
                           DebtType type,
                           BigDecimal originalAmount,
                           LocalDate dueDate,
                           BigDecimal monthlyInstallmentAmount,
                           LocalDate firstInstallmentDate,
                           String note) {
        boolean installmentRequested = monthlyInstallmentAmount != null || firstInstallmentDate != null;
        if (!installmentRequested && dueDate == null) {
            throw new IllegalArgumentException("Hạn thanh toán không được để trống.");
        }
        if (installmentRequested && (monthlyInstallmentAmount == null || firstInstallmentDate == null)) {
            throw new IllegalArgumentException("Cần gửi đủ số tiền mỗi tháng và ngày trả kỳ đầu.");
        }

        // Constructor tự suy ra hạn cuối từ ngày kỳ đầu và số kỳ trả góp.
        Debt debt = new Debt(currentUserService.getCurrentUser(), personName, type, originalAmount,
                dueDate == null ? firstInstallmentDate : dueDate, note,
                monthlyInstallmentAmount, firstInstallmentDate);
        return debtRepository.save(debt);
    }

    public Debt updateDebtById(int id,
                               String personName,
                               LocalDate dueDate,
                               Boolean installmentEnabled,
                               BigDecimal monthlyInstallmentAmount,
                               LocalDate firstInstallmentDate,
                               String note) {
        Debt debt = getDebtById(id);
        if (personName != null) {
            debt.setPersonName(personName);
        }

        if (installmentEnabled != null) {
            boolean planChanged = debt.hasInstallmentPlan() != installmentEnabled
                    || (installmentEnabled && (!Objects.equals(debt.getMonthlyInstallmentAmount(), monthlyInstallmentAmount)
                    || !Objects.equals(debt.getFirstInstallmentDate(), firstInstallmentDate)));
            if (planChanged && debtPaymentRepository.existsByDebtId(id)) {
                throw new IllegalArgumentException("Không thể đổi lịch trả góp sau khi đã có thanh toán.");
            }

            if (installmentEnabled) {
                debt.configureInstallmentPlan(monthlyInstallmentAmount, firstInstallmentDate);
            } else {
                if (dueDate == null) {
                    throw new IllegalArgumentException("Hạn thanh toán không được để trống.");
                }
                debt.configureInstallmentPlan(null, null);
                debt.setDueDate(dueDate);
            }
        } else if (dueDate != null) {
            if (debt.hasInstallmentPlan()) {
                throw new IllegalArgumentException("Lịch trả góp tự tính hạn cuối. Hãy chỉnh ngày trả kỳ đầu.");
            }
            debt.setDueDate(dueDate);
        }

        if (note != null) {
            debt.setNote(note);
        }

        return debtRepository.save(debt);
    }

    public void deleteDebtById(int id) {
        Debt debt = getDebtById(id);
        if (debtPaymentRepository.existsByDebtId(id))
            throw new IllegalArgumentException("Không thể xoá nợ đã có thanh toán.");
        debtRepository.delete(debt);
    }

    @Transactional(readOnly = true)
    public List<DebtAlertResponse> getDebtAlerts(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("Số ngày cảnh báo không được âm.");
        }

        LocalDate today = LocalDate.now();
        LocalDate lastAlertDate = today.plusDays(days);
        List<Debt> debts = debtRepository.findByUserIdAndStatus(
                currentUserService.getCurrentUser().getId(), DebtStatus.OPEN
        );

        return debts.stream().map(
                debt -> {
                    LocalDate nextDueDate = debt.hasInstallmentPlan()
                            ? debt.getNextInstallmentDate() : debt.getDueDate();
                    BigDecimal scheduledAmount = debt.hasInstallmentPlan()
                            ? debt.getNextInstallmentAmount() : debt.getRemainingAmount();
                    long daysUntilDue = ChronoUnit.DAYS.between(
                            today,
                            nextDueDate
                    );
                    boolean overdue = daysUntilDue < 0;
                    return new DebtAlertResponse(
                            debt.getId(),
                            debt.getPersonName(),
                            debt.getType(),
                            debt.getRemainingAmount(),
                            scheduledAmount,
                            nextDueDate,
                            daysUntilDue, overdue,
                            debt.hasInstallmentPlan()
                    );
                }
        ).filter(alert -> !alert.getDueDate().isAfter(lastAlertDate))
                .sorted(Comparator.comparing(DebtAlertResponse::getDueDate).thenComparingInt(DebtAlertResponse::getDebtId))
                .toList();
    }
}
