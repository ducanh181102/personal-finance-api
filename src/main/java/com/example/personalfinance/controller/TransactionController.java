package com.example.personalfinance.controller;

import com.example.personalfinance.dto.common.PageResponse;
import com.example.personalfinance.dto.transaction.CreateTransactionRequest;
import com.example.personalfinance.dto.transaction.TransactionResponse;
import com.example.personalfinance.dto.transaction.UpdateTransactionRequest;
import com.example.personalfinance.mapper.TransactionMapper;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionSource;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("/page")
    public PageResponse<TransactionResponse> getTransactionsPage(
            @RequestParam(value = "accountId", required = false) Integer accountId,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "source", required = false) TransactionSource source,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false, defaultValue = "DESC") String sort,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "22") int size) {

        Page<Transaction> transactions = transactionService.getTransactionsPage(page, size, accountId, categoryId,
                type,
                source,
                minAmount,
                maxAmount,
                keyword,
                sort,
                from,
                to);

        Page<TransactionResponse> responsePage =
                transactions.map(transactionMapper::toTransactionResponse);

        return new PageResponse<>(responsePage.getContent(),
                responsePage.getNumber(), responsePage.getSize(), responsePage.getTotalElements(),
                responsePage.getTotalPages(), responsePage.isFirst(), responsePage.isLast());
    }

    @GetMapping("/category/{categoryId}")
    public List<TransactionResponse> getTransactionsByCategoryId(@PathVariable("categoryId") int categoryId) {
        return transactionService.getTransactionsByCategoryId(categoryId).stream().map(transactionMapper::toTransactionResponse).toList();
    }

    @GetMapping
    public List<TransactionResponse> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return transactions.stream().map(transactionMapper::toTransactionResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable("id") int id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transactionMapper.toTransactionResponse(transaction));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request.getAccountId(), request.getCategoryId(), request.getType(), request.getAmount(), request.getNote(), request.getTransactionDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toTransactionResponse(transaction));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable("id") int id, @Valid @RequestBody UpdateTransactionRequest request) {
        Transaction transaction = transactionService.updateTransaction(id, request.getAmount(), request.getType(), request.getNote());
        return ResponseEntity.ok(transactionMapper.toTransactionResponse(transaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") int id) {
        transactionService.deleteTransactionById(id);
        return ResponseEntity.noContent().build();
    }
}
