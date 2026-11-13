package com.example.personalfinance.controller;

import com.example.personalfinance.dto.common.PageResponse;
import com.example.personalfinance.dto.transaction.CreateTransactionRequest;
import com.example.personalfinance.dto.transaction.TransactionResponse;
import com.example.personalfinance.dto.transaction.UpdateTransactionRequest;
import com.example.personalfinance.mapper.TransactionMapper;
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
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("/page")
    public PageResponse<TransactionResponse> getTransactionsPage(
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

        Page<Transaction> transactions = from != null ? transactionService.findPageByAccountIdWithinPeriod(page, size, accountId, from, to) :
                transactionService.getTransactionsPage(page, size, accountId);

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
    public List<TransactionResponse> getAllTransactions(@RequestParam(value = "accountId", required = false) Integer accountId,
                                                        @RequestParam(value = "from", required = false) LocalDate from,
                                                        @RequestParam(value = "to", required = false) LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Cần gửi cả ngày bắt đầu và ngày kết thúc");
        }

        if (from != null && accountId == null) {
            throw new IllegalArgumentException("Cần gửi accountId khi lọc theo ngày");
        }

        List<Transaction> transactions = accountId == null ? transactionService.getAllTransactions() : from != null ?
                transactionService.findByAccountIdWithinPeriod(accountId, from, to) :
                transactionService.getTransactionsByAccountId(accountId);

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
