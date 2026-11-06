package com.example.personalfinance.controller;

import com.example.personalfinance.dto.CreateAccountRequest;
import com.example.personalfinance.dto.UpdateAccountNameRequest;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    private final AccountService accountService;

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable("id") int id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccountById(@PathVariable("id") int id) {
        accountService.deleteAccountById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.getName(), request.getType(), request.getOpeningBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable("id") int id, @Valid @RequestBody UpdateAccountNameRequest request) {
        Account account = accountService.updateAccountById(id, request.getName());
        return ResponseEntity.ok(account);
    }
}
