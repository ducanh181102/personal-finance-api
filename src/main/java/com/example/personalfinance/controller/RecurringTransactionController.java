package com.example.personalfinance.controller;

import com.example.personalfinance.dto.recurring.CreateRecurringTransactionRequest;
import com.example.personalfinance.dto.recurring.RecurringTransactionResponse;
import com.example.personalfinance.dto.recurring.UpdateRecurringTransactionRequest;
import com.example.personalfinance.mapper.RecurringTransactionMapper;
import com.example.personalfinance.service.RecurringTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {
    private final RecurringTransactionService recurringTransactionService;
    private final RecurringTransactionMapper recurringTransactionMapper;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService, RecurringTransactionMapper recurringTransactionMapper) {
        this.recurringTransactionService = recurringTransactionService;
        this.recurringTransactionMapper = recurringTransactionMapper;
    }

    @GetMapping
    public List<RecurringTransactionResponse> getAllRecurringTransactions() {
        return recurringTransactionService.getAllRecurringTransactions().stream().map(recurringTransactionMapper::toRecurringTransactionResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> getRecurringTransactionById(@PathVariable("id") int id) {
        RecurringTransactionResponse recurringTransactionResponse = recurringTransactionMapper.toRecurringTransactionResponse(recurringTransactionService.getRecurringTransactionById(id));
        return ResponseEntity.ok(recurringTransactionResponse);
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(@Valid @RequestBody CreateRecurringTransactionRequest request) {
        RecurringTransactionResponse recurringTransactionResponse = recurringTransactionMapper.toRecurringTransactionResponse(recurringTransactionService.createRecurringTransaction(request.getAccountId(),
                request.getCategoryId(),
                request.getAmount(),
                request.getNote(),
                request.getFrequency(),
                request.getNextRunDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringTransactionResponse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> updateRecurringTransactionById(@PathVariable("id") int id, @Valid @RequestBody UpdateRecurringTransactionRequest request) {
        RecurringTransactionResponse recurringTransactionResponse = recurringTransactionMapper.toRecurringTransactionResponse(
                recurringTransactionService.updateRecurringTransactionById(id,
                        request.getAccountId(),
                        request.getCategoryId(),
                        request.getAmount(),
                        request.getNote(),
                        request.getFrequency(),
                        request.getNextRunDate(),
                        request.isActive())
        );
        return ResponseEntity.ok(recurringTransactionResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransactionById(@PathVariable("id") int id) {
        recurringTransactionService.deleteRecurringTransactionById(id);
        return ResponseEntity.noContent().build();
    }
}
