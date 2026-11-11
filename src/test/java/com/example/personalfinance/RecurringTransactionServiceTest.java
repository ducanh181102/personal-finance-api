package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.RecurrenceFrequency;
import com.example.personalfinance.model.RecurringTransaction;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.RecurringTransactionRepository;
import com.example.personalfinance.repository.TransactionRepository;
import com.example.personalfinance.repository.UserRepository;
import com.example.personalfinance.service.RecurringTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recurring-transaction-service-test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
class RecurringTransactionServiceTest {

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void createsTransactionsForEveryMissedMonthAndAdvancesNextRunDate() {
        LocalDate today = LocalDate.now();
        LocalDate firstRunDate = today.withDayOfMonth(1).minusMonths(3);

        User user = userRepository.save(
                new User("Recurring test", "recurring@example.com", "password-hash")
        );
        Account account = accountRepository.save(
                new Account("Ví test", "CASH", BigDecimal.ZERO, user)
        );
        Category category = categoryRepository.save(
                new Category("Tiền trọ", TransactionType.EXPENSE, user)
        );

        RecurringTransaction activeRecurring = recurringTransactionRepository.save(
                new RecurringTransaction(
                        account,
                        category,
                        BigDecimal.valueOf(3_000_000),
                        "Tiền trọ hằng tháng",
                        RecurrenceFrequency.MONTHLY,
                        firstRunDate
                )
        );

        RecurringTransaction inactiveRecurring = new RecurringTransaction(
                account,
                category,
                BigDecimal.valueOf(100_000),
                "Mẫu đã tắt",
                RecurrenceFrequency.MONTHLY,
                firstRunDate
        );
        inactiveRecurring.setActive(false);
        recurringTransactionRepository.save(inactiveRecurring);

        recurringTransactionService.runDueRecurringTransactions();

        List<Transaction> createdTransactions = transactionRepository.findByAccountId(account.getId());

        assertEquals(4, createdTransactions.size());
        assertEquals(
                List.of(
                        firstRunDate,
                        firstRunDate.plusMonths(1),
                        firstRunDate.plusMonths(2),
                        firstRunDate.plusMonths(3)
                ),
                createdTransactions.stream()
                        .map(transaction -> transaction.getTransactionDate().toLocalDate())
                        .sorted()
                        .toList()
        );
        assertEquals(
                new BigDecimal("12000000.00"),
                createdTransactions.stream()
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        RecurringTransaction updatedRecurring = recurringTransactionRepository
                .findById(activeRecurring.getId())
                .orElseThrow();
        assertEquals(firstRunDate.plusMonths(4), updatedRecurring.getNextRunDate());
    }
}
