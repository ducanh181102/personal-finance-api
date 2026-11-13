package com.example.personalfinance.service;

import com.example.personalfinance.dto.account.AccountBalanceResponse;
import com.example.personalfinance.dto.account.AccountPeriodSummaryResponse;
import com.example.personalfinance.dto.account.AccountSummaryResponse;
import com.example.personalfinance.dto.category.CategorySummaryResponse;
import com.example.personalfinance.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountBalanceService {

    private final TransactionService transactionService;
    private final TransferService transferService;
    private final AccountService accountService;

    public AccountBalanceService(TransactionService transactionService, TransferService transferService, AccountService accountService) {
        this.transactionService = transactionService;
        this.transferService = transferService;
        this.accountService = accountService;
    }

    @Transactional
    public AccountBalanceResponse getCurrentBalanceByAccountId(int accountId) {
        BigDecimal balanceFromTransactions = getBalanceFromTransactionsByAccountId(accountId).getCurrentBalance();
        BigDecimal balanceFromTransfer = getBalanceFromTransfersByToAccountId(accountId)
                .getCurrentBalance().add(getBalanceFromTransfersByFromAccountId(accountId).getCurrentBalance());
        BigDecimal openingBalance = accountService.getAccountById(accountId).getOpeningBalance();
        BigDecimal currentBalance = openingBalance.add(balanceFromTransactions.add(balanceFromTransfer));
        return new AccountBalanceResponse(accountId, currentBalance);
    }

    @Transactional
    public AccountBalanceResponse getBalanceFromTransactionsByAccountId(int accountId) {
        // Tìm account đối tượng
        Account account = accountService.getAccountById(accountId);

        // Số tiền sau giao dịch
        BigDecimal balance = BigDecimal.ZERO;

        // Lấy các transactions của account đối tượng
        List<Transaction> transactions = transactionService.getTransactionsByAccountId(account.getId());

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

        // Tổng chi
        BigDecimal totalExpense = new BigDecimal(0);

        // Tổng thu
        BigDecimal totalIncome = new BigDecimal(0);

        // Lấy các transactions của account đối tượng
        List<Transaction> transactions = transactionService.getTransactionsByAccountId(account.getId());

        // Duyệt qua transactions
        for (Transaction transaction : transactions) {
            // Loại giao dịch
            TransactionType type = transaction.getType();
            // Số tiền thanh toán
            BigDecimal amount = transaction.getAmount();
            // Công thức tính số tiền sau giao dịch
            if (type == TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(amount);
            } else {
                totalIncome = totalIncome.add(amount);
            }
        }

        // Số tiền sau giao dịch
        BigDecimal balance = getCurrentBalanceByAccountId(accountId).getCurrentBalance();

        // Trả trị kết quả
        return new AccountSummaryResponse(account.getId(), totalIncome, totalExpense, balance);
    }

    public List<CategorySummaryResponse> getCategorySummaryByAccountId(int accountId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionService.findByAccountIdWithinPeriod(accountId, from, to);

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

    @Transactional
    public AccountPeriodSummaryResponse getAccountPeriodSummary(int accountId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionService.findByAccountIdWithinPeriod(accountId, from, to);

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

    @Transactional
    public AccountBalanceResponse getBalanceFromTransfersByFromAccountId(int fromAccountId) {
        // Tìm account đối tượng
        Account account = accountService.getAccountById(fromAccountId);

        // Số tiền sau giao dịch
        BigDecimal balance = BigDecimal.ZERO;

        // Lấy các transfer của account đối tượng
        List<Transfer> transfers = transferService.getAllTransferByFromAccountId(account.getId());

        // Duyệt qua transfers
        for (Transfer transfer : transfers) {
            // Số tiền giao dịch
            BigDecimal amount = transfer.getAmount();
            // Công thức tính số tiền sau giao dịch
            balance = balance.subtract(amount);
        }

        // Trả trị kết quả
        return new AccountBalanceResponse(account.getId(), balance);
    }

    @Transactional
    public AccountBalanceResponse getBalanceFromTransfersByToAccountId(int toAccountId) {
        // Tìm account đối tượng
        Account account = accountService.getAccountById(toAccountId);

        // Số tiền sau giao dịch
        BigDecimal balance = BigDecimal.ZERO;

        // Lấy các transfer của account đối tượng
        List<Transfer> transfers = transferService.getAllTransferByToAccountId(account.getId());

        // Duyệt qua transfers
        for (Transfer transfer : transfers) {
            // Số tiền giao dịch
            BigDecimal amount = transfer.getAmount();
            // Công thức tính số tiền sau giao dịch
            balance = balance.add(amount);
        }

        // Trả trị kết quả
        return new AccountBalanceResponse(account.getId(), balance);
    }
}
