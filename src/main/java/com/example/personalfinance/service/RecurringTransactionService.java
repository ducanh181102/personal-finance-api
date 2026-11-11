package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.*;
import com.example.personalfinance.repository.RecurringTransactionRepository;
import com.example.personalfinance.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RecurringTransactionService {
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;

    public RecurringTransactionService(RecurringTransactionRepository recurringTransactionRepository,
                                       CurrentUserService currentUserService,
                                       AccountService accountService,
                                       CategoryService categoryService,
                                       TransactionRepository transactionRepository) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.transactionRepository = transactionRepository;
    }

    private RecurringTransaction getOwnedRecurringTransactionById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<RecurringTransaction> recurringTransaction = recurringTransactionRepository.findById(id);
        if (recurringTransaction.isEmpty()) throw new NotFoundException(id);
        if (recurringTransaction.get().getCategory().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return recurringTransaction.get();
    }

    public List<RecurringTransaction> getAllRecurringTransactions() {
        return recurringTransactionRepository.findByAccountUserId(currentUserService.getCurrentUser().getId());
    }

    public RecurringTransaction getRecurringTransactionById(int id) {
        return getOwnedRecurringTransactionById(id);
    }

    public RecurringTransaction createRecurringTransaction(int accountId, int categoryId,
                                                           BigDecimal amount, String note,
                                                           RecurrenceFrequency frequency,
                                                           LocalDate nextRunDate) {
        Account account = accountService.getAccountById(accountId);
        Category category = categoryService.findCategoryById(categoryId);
        RecurringTransaction recurringTransaction = new RecurringTransaction(account, category, amount, note, frequency, nextRunDate);
        return recurringTransactionRepository.save(recurringTransaction);
    }

    public RecurringTransaction updateRecurringTransactionById(int id, Integer accountId, Integer categoryId,
                                                               BigDecimal amount, String note,
                                                               RecurrenceFrequency frequency,
                                                               LocalDate nextRunDate, Boolean active) {
        RecurringTransaction recurringTransaction = getRecurringTransactionById(id);

        if (accountId != null) {
            Account account = accountService.getAccountById(accountId);
            recurringTransaction.setAccount(account);
        }

        if (categoryId != null) {
            Category category = categoryService.findCategoryById(categoryId);
            recurringTransaction.setCategory(category);
        }

        if (amount != null) {
            recurringTransaction.setAmount(amount);
        }

        if (note != null) {
            recurringTransaction.setNote(note);
        }

        if (frequency != null) {
            recurringTransaction.setFrequency(frequency);
        }

        if (nextRunDate != null) {
            recurringTransaction.setNextRunDate(nextRunDate);
        }

        if (active != null) {
            recurringTransaction.setActive(active);
        }

        return recurringTransactionRepository.save(recurringTransaction);
    }

    public void deleteRecurringTransactionById(int id) {
        RecurringTransaction recurringTransaction = getOwnedRecurringTransactionById(id);
        recurringTransactionRepository.delete(recurringTransaction);
    }

    private List<RecurringTransaction> getRecurringTransactionByActiveTrueAndNextRunDateLessThanEqual(LocalDate today) {
        return recurringTransactionRepository.findByActiveTrueAndNextRunDateLessThanEqual(today);
    }

    private void createTransactionByDueRecurringTransaction(RecurringTransaction dueRecurringTransaction) {
        transactionRepository.save(new Transaction(
                dueRecurringTransaction.getAccount(),
                dueRecurringTransaction.getCategory().getType(),
                dueRecurringTransaction.getAmount(),
                dueRecurringTransaction.getNextRunDate().atStartOfDay(),
                dueRecurringTransaction.getNote(),
                dueRecurringTransaction.getCategory()
        ));

        dueRecurringTransaction.setNextRunDate(
                dueRecurringTransaction.getNextRunDate().plusMonths(1));

    }

    @Transactional
    public void runDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueRecurringTransactions = getRecurringTransactionByActiveTrueAndNextRunDateLessThanEqual(today);
        for (RecurringTransaction recurringTransaction : dueRecurringTransactions) {
            do {
                createTransactionByDueRecurringTransaction(recurringTransaction);
                // Xử lý tạo bù, trường hợp server đã chết khá lâu, nhiều transaction trong quá khứ đã bị bỏ qua
            } while (!recurringTransaction.getNextRunDate().isAfter(today));
        }
    }
}
