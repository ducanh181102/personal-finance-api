package com.example.personalfinance.service;

import com.example.personalfinance.dto.AccountBalanceResponse;
import com.example.personalfinance.dto.AccountPeriodSummaryResponse;
import com.example.personalfinance.dto.AccountSummaryResponse;
import com.example.personalfinance.dto.CategorySummaryResponse;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, AccountService accountService, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(int id) {
        Optional<Transaction> transaction = transactionRepository.findById(id);
        if (transaction.isEmpty()) {
            throw new NotFoundException(id);
        } else return transaction.get();
    }

    @Transactional
    public Transaction updateTransaction(int id, BigDecimal amount, TransactionType type, String note) {
        Transaction transaction = getTransactionById(id);
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
        if (category.getType() != type)
            throw new IllegalArgumentException("Loại danh mục phải trùng với loại giao dịch");
        Transaction transaction = new Transaction(account, type, amount, transactionDate, note, category);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransactionById(int id) {
        Transaction transaction = getTransactionById(id);
        transactionRepository.delete(transaction);
    }

    @Transactional
    public List<Transaction> getTransactionsByAccountId(int accountId) {
        accountService.getAccountById(accountId);
        return transactionRepository.findByAccountId(accountId);
    }

    @Transactional
    public AccountBalanceResponse getCurrentBalanceByAccountId(int accountId) {
        // Tìm account đối tượng
        Account account = accountService.getAccountById(accountId);

        // Số tiền sau giao dịch
        BigDecimal balance = account.getOpeningBalance();

        // Lấy các transactions của account đối tượng
        List<Transaction> transactions = getTransactionsByAccountId(account.getId());

        // Duyệt qua transactions
        for (Transaction transaction : transactions) {
            // Loại giao dịch
            TransactionType type = transaction.getType();
            // Số tiền thanh toán
            BigDecimal amount = transaction.getAmount();
            // Công thức tính số tiền sau giao dịch
            balance = type != TransactionType.EXPENSE ? balance.add(amount) : balance.subtract(amount);
        }

        // Trả trị kết quả
        return new AccountBalanceResponse(account.getId(), balance);
    }

    @Transactional
    public AccountSummaryResponse getAccountSummaryByAccountId(int accountId) {
        // Tìm account đối tượng
        Account account = accountService.getAccountById(accountId);

        // Số tiền sau giao dịch
        BigDecimal balance = account.getOpeningBalance();

        // Tổng chi
        BigDecimal totalExpense = new BigDecimal(0);

        // Tổng thu
        BigDecimal totalIncome = new BigDecimal(0);

        // Lấy các transactions của account đối tượng
        List<Transaction> transactions = getTransactionsByAccountId(account.getId());

        // Duyệt qua transactions
        for (Transaction transaction : transactions) {
            // Loại giao dịch
            TransactionType type = transaction.getType();
            // Số tiền thanh toán
            BigDecimal amount = transaction.getAmount();
            // Công thức tính số tiền sau giao dịch
            if (type == TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(amount);
                balance = balance.subtract(amount);
            } else {
                totalIncome = totalIncome.add(amount);
                balance = balance.add(amount);
            }
        }

        // Trả trị kết quả
        return new AccountSummaryResponse(account.getId(), totalIncome, totalExpense, balance);
    }

    @Transactional
    public List<Transaction> findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int accountId, LocalDate from, LocalDate to) {
        accountService.getAccountById(accountId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, start, end);
    }

    @Transactional
    public List<Transaction> findByCategoryIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(int categoryId, LocalDate from, LocalDate to) {
        categoryService.findCategoryById(categoryId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByCategoryIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(categoryId, start, end);
    }

    @Transactional
    public AccountPeriodSummaryResponse getAccountPeriodSummary(int accountId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, from, to);

        // Tổng thu
        BigDecimal totalIncome = BigDecimal.ZERO;
        // Tổng chi
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            totalIncome = totalIncome.add(transaction.getType() == TransactionType.EXPENSE ? BigDecimal.ZERO : transaction.getAmount());
            totalExpense = totalExpense.add(transaction.getType() != TransactionType.EXPENSE ? BigDecimal.ZERO : transaction.getAmount());
        }
        // Mức chênh lệch
        BigDecimal netChange = totalIncome.subtract(totalExpense);

        return new AccountPeriodSummaryResponse(accountId, from, to, totalIncome, totalExpense, netChange);
    }

    public Page<Transaction> getTransactionsPage(int page, int size, Integer accountId) {
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(
                        Sort.Order.desc("transactionDate"),
                        Sort.Order.desc("id")
                ));
        if (accountId == null) {
            return transactionRepository.findAll(pageRequest);
        } else {
            accountService.getAccountById(accountId);
            return transactionRepository.findByAccountId(accountId, pageRequest);
        }
    }

    public Page<Transaction> findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThanPage(int page, int size, int accountId, LocalDate from, LocalDate to) {
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

    public List<CategorySummaryResponse> getCategorySummaryByAccountId(int accountId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, from, to);

        Map<Category, BigDecimal> totalByCategory = transactions.stream().collect(
                Collectors.groupingBy(Transaction::getCategory, Collectors.reducing(
                        BigDecimal.ZERO,
                        Transaction::getAmount,
                        BigDecimal::add
                ))
        );

        List<CategorySummaryResponse> responses = new ArrayList<>();

        for (Map.Entry<Category, BigDecimal> entry : totalByCategory.entrySet()) {
            Category category = entry.getKey();
            BigDecimal total = entry.getValue();
            CategorySummaryResponse categorySummaryResponse = new CategorySummaryResponse(
                    category.getId(),
                    category.getName(),
                    category.getType(),
                    total
            );

            responses.add(categorySummaryResponse);
        }

        return responses;
    }
}
