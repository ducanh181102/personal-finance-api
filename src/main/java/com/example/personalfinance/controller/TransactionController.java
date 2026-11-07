package com.example.personalfinance.controller;

import com.example.personalfinance.dto.CreateTransactionRequest;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.service.TransactionService;
import jakarta.validation.Valid;
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
        Transaction transaction = transactionService.createTransaction(request.getAccountId(), request.getType(), request.getAmount(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") int id) {
        transactionService.deleteTransactionById(id);
        return ResponseEntity.noContent().build();
    }


}
