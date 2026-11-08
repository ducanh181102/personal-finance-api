package com.example.personalfinance.controller;

import com.example.personalfinance.dto.CreateTransactionRequest;
import com.example.personalfinance.dto.UpdateTransactionRequest;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/page")
    public Page<Transaction> getTransactionsPage(
            @RequestParam(value = "accountId", required = false) Integer accountId,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Cần gửi cả ngày bắt đầu và ngày kết thúc");
        }

        if (from != null && accountId == null) {
            throw new IllegalArgumentException("Cần gửi accountId khi lọc theo ngày");
        }

        if (page < 0) {
            throw new IllegalArgumentException("Vị trí trang không được nhỏ hơn 0");
        } else if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Kích thước tối đa của 1 trang phải nằm trong khoảng từ 1 đến 100");
        }

        return from != null ? transactionService.findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThanPage(page, size, accountId, from, to) :
                transactionService.getTransactionsPage(page, size, accountId);
    }

    @GetMapping
    public List<Transaction> getAllTransactions(@RequestParam(value = "accountId", required = false) Integer accountId,
                                                @RequestParam(value = "from", required = false) LocalDate from,
                                                @RequestParam(value = "to", required = false) LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Cần gửi cả ngày bắt đầu và ngày kết thúc");
        }

        if (from != null && accountId == null) {
            throw new IllegalArgumentException("Cần gửi accountId khi lọc theo ngày");
        }

        return accountId == null ? transactionService.getAllTransactions() : from != null ?
                transactionService.findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, from, to) :
                transactionService.getTransactionsByAccountId(accountId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable("id") int id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request.getAccountId(), request.getCategoryId(), request.getType(), request.getAmount(), request.getNote(), request.getTransactionDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable("id") int id, @Valid @RequestBody UpdateTransactionRequest request) {
        Transaction transaction = transactionService.updateTransaction(id, request.getAmount(), request.getType(), request.getNote());
        return ResponseEntity.ok(transaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") int id) {
        transactionService.deleteTransactionById(id);
        return ResponseEntity.noContent().build();
    }
}
