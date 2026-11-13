package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.*;
import com.example.personalfinance.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository, AccountService accountService, CategoryService categoryService, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    private Transaction getOwnedTransactionById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Transaction> transaction = transactionRepository.findById(id);
        if (transaction.isEmpty()) throw new NotFoundException(id);
        if (transaction.get().getCategory().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return transaction.get();
    }

    public List<Transaction> getAllTransactions() {
        User currentUser = currentUserService.getCurrentUser();
        return transactionRepository.findByAccountUserId(currentUser.getId());
    }

    public Transaction getTransactionById(int id) {
        return getOwnedTransactionById(id);
    }

    @Transactional
    public Transaction updateTransaction(int id, BigDecimal amount, TransactionType type, String note) {
        Transaction transaction = getOwnedTransactionById(id);
        transaction.setAmount(amount != null ? amount : transaction.getAmount());
        transaction.setNote(note != null ? note : transaction.getNote());
        if (type != null && type != transaction.getCategory().getType()) {
            throw new IllegalArgumentException(
                    "Loại giao dịch phải trùng với loại danh mục"
            );
        }
        transaction.setType(type != null ? type : transaction.getType());
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createTransaction(int accountId,
                                         int categoryId,
                                         TransactionType type,
                                         BigDecimal amount,
                                         String note,
                                         LocalDateTime transactionDate) {
        Account account = accountService.getAccountById(accountId);
        Category category = categoryService.findCategoryById(categoryId);

        // Nghiệp vụ: kiểm tra account và category phải thuộc cùng 1 user
        if (account.getUser().getId() != category.getUser().getId())
            throw new IllegalArgumentException("Không cho giao dịch dùng category của user khác.");
        if (category.getType() != type)
            throw new IllegalArgumentException("Loại danh mục phải trùng với loại giao dịch");
        Transaction transaction = new Transaction(account, type, amount, transactionDate, note, category);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransactionById(int id) {
        Transaction transaction = getOwnedTransactionById(id);
        transactionRepository.delete(transaction);
    }

    @Transactional
    public List<Transaction> getTransactionsByAccountId(int accountId) {
        accountService.getAccountById(accountId);
        return transactionRepository.findByAccountId(accountId);
    }

    @Transactional
    public List<Transaction> findByAccountIdWithinPeriod(int accountId, LocalDate from, LocalDate to) {
        accountService.getAccountById(accountId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, start, end);
    }

    @Transactional
    public List<Transaction> findByCategoryIdWithinPeriod(int categoryId, LocalDate from, LocalDate to) {
        categoryService.findCategoryById(categoryId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByCategoryIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(categoryId, start, end);
    }

    public Page<Transaction> getTransactionsPage(int page, int size, Integer accountId) {
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(
                        Sort.Order.desc("transactionDate"),
                        Sort.Order.desc("id")
                ));
        if (accountId == null) {
            User currentUser = currentUserService.getCurrentUser();
            return transactionRepository.findByAccountUserId(currentUser.getId(), pageRequest);
        } else {
            accountService.getAccountById(accountId);
            return transactionRepository.findByAccountId(accountId, pageRequest);
        }
    }

    public Page<Transaction> findPageByAccountIdWithinPeriod(int page, int size, int accountId, LocalDate from, LocalDate to) {
        accountService.getAccountById(accountId);
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(
                        Sort.Order.desc("transactionDate"),
                        Sort.Order.desc("id")
                ));
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, start, end, pageRequest);
    }

    @Transactional
    public List<Transaction> getTransactionsByCategoryId(int categoryId) {
        categoryService.findCategoryById(categoryId);
        return transactionRepository.findByCategoryId(categoryId);
    }
}
