package com.example.personalfinance.service;

import com.example.personalfinance.exception.AccountNotFoundException;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.repository.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    private final AccountRepository accountRepository;

    public Account createAccount(String name, String type, BigDecimal openingBalance) {
        Account account = new Account(name, type, openingBalance);
        return accountRepository.save(account);
    }

    public Account updateAccountById(int id, String name) {
        Account account = getAccountById(id);
        account.setName(name);
        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(int id) {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isEmpty()) throw new AccountNotFoundException(id);
        else return account.get();
    }

    public void deleteAccountById(int id) {
        Account account = getAccountById(id);
        accountRepository.delete(account);
    }
}
