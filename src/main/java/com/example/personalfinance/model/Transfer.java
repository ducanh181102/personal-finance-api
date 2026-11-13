package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;
    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDateTime transferDate;
    @Column(nullable = true, length = 255)
    private String note;

    public Transfer(Account fromAccount, Account toAccount, BigDecimal amount, LocalDateTime transferDate, String note) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.transferDate = transferDate;
        this.note = note;
    }

    protected Transfer() {
    }

    public int getId() {
        return id;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTransferDate() {
        return transferDate;
    }

    public String getNote() {
        return note;
    }
}
