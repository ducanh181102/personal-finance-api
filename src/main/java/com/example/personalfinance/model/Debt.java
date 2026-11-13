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
import java.time.LocalDate;

@Entity
@Table(name = "debts")
public class Debt {
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
        this.user = user;
        this.personName = personName;
        this.type = type;
        this.originalAmount = originalAmount;
        this.remainingAmount = originalAmount;
        this.dueDate = dueDate;
        this.status = DebtStatus.OPEN;
        this.note = note;
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

    public DebtStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
