package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.RecurringTransactionRepository;
import com.example.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    private final CurrentUserService currentUserService;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public AccountService(AccountRepository accountRepository, CurrentUserService currentUserService, TransactionRepository transactionRepository, RecurringTransactionRepository recurringTransactionRepository) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.transactionRepository = transactionRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
    }

    private final AccountRepository accountRepository;

    private Account getOwnedAccountById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Account> account = accountRepository.findById(id);
        if (account.isEmpty()) throw new NotFoundException(id);
        if (account.get().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return account.get();
    }

    public Account createAccount(String name, String type, BigDecimal openingBalance) {
        User user = currentUserService.getCurrentUser();
        Account account = new Account(name, type, openingBalance, user);
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccountById(int id, String name) {
        Account account = getOwnedAccountById(id);
        account.setName(name);
        return accountRepository.save(account);
    }

    @Transactional
    public List<Account> getAllAccounts() {
        User currentUser = currentUserService.getCurrentUser();
        return accountRepository.findByUserId(currentUser.getId());
    }

    public Account getAccountById(int id) {
        return getOwnedAccountById(id);
    }

    @Transactional
    public void deleteAccountById(int id) {
        Account account = getOwnedAccountById(id);
        if (transactionRepository.existsByAccountId(id))
            throw new IllegalArgumentException("Không được xóa tài khoản đã có giao dịch.");
        if (recurringTransactionRepository.existsByAccountId(id))
            throw new IllegalArgumentException("Không được xóa tài khoản đang được dùng bởi giao dịch định kỳ.");

        accountRepository.delete(account);
    }

    // Đình chỉ
//    @Transactional
//    private List<Account> getAccountsByUserId(int userId) {
//        userService.getUserById(userId);
//        return accountRepository.findByUserId(userId);
//    }
}
