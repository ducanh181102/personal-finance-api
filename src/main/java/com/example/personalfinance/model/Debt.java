package com.example.personalfinance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "debts")
public class Debt {
    private static final long MAX_INSTALLMENT_COUNT = 1_200L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String personName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebtType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    // Hai trường cùng null nghĩa là khoản nợ thanh toán một lần như trước đây.
    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyInstallmentAmount;

    private LocalDate firstInstallmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebtStatus status;

    @Column(length = 255)
    private String note;

    protected Debt() {
    }

    public Debt(User user,
                String personName,
                DebtType type,
                BigDecimal originalAmount,
                LocalDate dueDate,
                String note) {
        this(user, personName, type, originalAmount, dueDate, note, null, null);
    }

    public Debt(User user,
                String personName,
                DebtType type,
                BigDecimal originalAmount,
                LocalDate dueDate,
                String note,
                BigDecimal monthlyInstallmentAmount,
                LocalDate firstInstallmentDate) {
        this.user = user;
        this.personName = personName;
        this.type = type;
        this.originalAmount = originalAmount;
        this.remainingAmount = originalAmount;
        this.dueDate = dueDate;
        this.status = DebtStatus.OPEN;
        this.note = note;
        configureInstallmentPlan(monthlyInstallmentAmount, firstInstallmentDate);
    }

    public void applyPayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }
        if (amount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Số tiền thanh toán vượt quá số nợ còn lại.");
        }

        remainingAmount = remainingAmount.subtract(amount);
        updateStatus();
    }

    public void reversePayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }

        remainingAmount = remainingAmount.add(amount);
        updateStatus();
    }

    private void updateStatus() {
        status = remainingAmount.compareTo(BigDecimal.ZERO) == 0
                ? DebtStatus.PAID
                : DebtStatus.OPEN;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void configureInstallmentPlan(BigDecimal monthlyAmount, LocalDate firstDate) {
        if (monthlyAmount == null && firstDate == null) {
            monthlyInstallmentAmount = null;
            firstInstallmentDate = null;
            return;
        }
        if (monthlyAmount == null || firstDate == null) {
            throw new IllegalArgumentException("Cần gửi đủ số tiền mỗi tháng và ngày trả kỳ đầu.");
        }
        if (monthlyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền trả góp mỗi tháng phải lớn hơn 0.");
        }
        if (monthlyAmount.compareTo(originalAmount) > 0) {
            throw new IllegalArgumentException("Số tiền trả góp mỗi tháng không được vượt số tiền gốc.");
        }

        long installmentCount = calculateInstallmentCount(originalAmount, monthlyAmount);
        if (installmentCount > MAX_INSTALLMENT_COUNT) {
            throw new IllegalArgumentException("Lịch trả góp không được vượt quá 1200 kỳ.");
        }
        monthlyInstallmentAmount = monthlyAmount;
        firstInstallmentDate = firstDate;
        // Hạn cuối là ngày của kỳ cuối, luôn được suy ra từ lịch thay vì tin giá trị client gửi lên.
        dueDate = firstDate.plusMonths(installmentCount - 1L);
    }

    public boolean hasInstallmentPlan() {
        return monthlyInstallmentAmount != null && firstInstallmentDate != null;
    }

    public LocalDate getNextInstallmentDate() {
        if (!hasInstallmentPlan() || status == DebtStatus.PAID) {
            return null;
        }
        BigDecimal paidAmount = originalAmount.subtract(remainingAmount);
        long fullyCoveredInstallments = paidAmount.divide(monthlyInstallmentAmount, 0, RoundingMode.DOWN).longValue();
        return firstInstallmentDate.plusMonths(fullyCoveredInstallments);
    }

    public BigDecimal getNextInstallmentAmount() {
        if (!hasInstallmentPlan() || status == DebtStatus.PAID) {
            return null;
        }
        BigDecimal paidAmount = originalAmount.subtract(remainingAmount);
        BigDecimal paidForCurrentInstallment = paidAmount.remainder(monthlyInstallmentAmount);
        BigDecimal amountLeftInCurrentInstallment = monthlyInstallmentAmount.subtract(paidForCurrentInstallment);
        return remainingAmount.min(amountLeftInCurrentInstallment);
    }

    public long getInstallmentCount() {
        return hasInstallmentPlan() ? calculateInstallmentCount(originalAmount, monthlyInstallmentAmount) : 0;
    }

    private static long calculateInstallmentCount(BigDecimal total, BigDecimal monthlyAmount) {
        BigDecimal[] division = total.divideAndRemainder(monthlyAmount);
        long wholeInstallments = division[0].longValueExact();
        return wholeInstallments + (division[1].signum() == 0 ? 0L : 1L);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getPersonName() {
        return personName;
    }

    public DebtType getType() {
        return type;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getMonthlyInstallmentAmount() {
        return monthlyInstallmentAmount;
    }

    public LocalDate getFirstInstallmentDate() {
        return firstInstallmentDate;
    }

    public DebtStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
