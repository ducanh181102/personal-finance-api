package com.example.personalfinance.controller;

import com.example.personalfinance.dto.*;
import com.example.personalfinance.mapper.AccountMapper;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.service.AccountService;
import com.example.personalfinance.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final TransactionService transactionService;
    private final AccountMapper accountMapper;

    public AccountController(TransactionService transactionService, AccountMapper accountMapper, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountMapper = accountMapper;
        this.accountService = accountService;
    }

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountService.getAllAccounts().stream().map(
                accountMapper::toAccountResponse
        ).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable("id") int id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(accountMapper.toAccountResponse(account));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<AccountBalanceResponse> getCurrentBalanceByAccountId(@PathVariable("id") int id) {
        AccountBalanceResponse accountBalanceResponse = transactionService.getCurrentBalanceByAccountId(id);
        return ResponseEntity.ok(accountBalanceResponse);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<AccountSummaryResponse> getSummaryByAccountId(@PathVariable("id") int id) {
        AccountSummaryResponse summaryResponse = transactionService.getAccountSummaryByAccountId(id);
        return ResponseEntity.ok(summaryResponse);
    }

    @GetMapping("/{id}/period-summary")
    public ResponseEntity<AccountPeriodSummaryResponse> getAccountPeriodSummaryByAccountId(@PathVariable("id") int id,
                                                                                           @RequestParam("from") LocalDate from,
                                                                                           @RequestParam("to") LocalDate to) {
        AccountPeriodSummaryResponse periodSummary = transactionService.getAccountPeriodSummary(id, from, to);
        return ResponseEntity.ok(periodSummary);
    }

    @GetMapping("/{id}/category-summary")
    public List<CategorySummaryResponse> getCategorySummaryByAccountId(@PathVariable("id") int id,
                                                                       @RequestParam("from") LocalDate from,
                                                                       @RequestParam("to") LocalDate to) {
        return transactionService.getCategorySummaryByAccountId(id, from, to);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccountById(@PathVariable("id") int id) {
        accountService.deleteAccountById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.getName(), request.getType(), request.getOpeningBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.toAccountResponse(account));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable("id") int id, @Valid @RequestBody UpdateAccountNameRequest request) {
        Account account = accountService.updateAccountById(id, request.getName());
        return ResponseEntity.ok(accountMapper.toAccountResponse(account));
    }
}
