package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.RecurrenceFrequency;
import com.example.personalfinance.model.RecurringTransaction;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionSource;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.RecurringTransactionRepository;
import com.example.personalfinance.repository.TransactionRepository;
import com.example.personalfinance.repository.UserRepository;
import com.example.personalfinance.security.JwtService;
import com.example.personalfinance.service.RecurringTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recurring-transaction-service-test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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
        assertTrue(createdTransactions.stream()
                .allMatch(transaction -> transaction.getSource() == TransactionSource.RECURRING));

        RecurringTransaction updatedRecurring = recurringTransactionRepository
                .findById(activeRecurring.getId())
                .orElseThrow();
        assertEquals(firstRunDate.plusMonths(4), updatedRecurring.getNextRunDate());
    }

    @Test
    void preventsUpdatingOrDeletingTransactionGeneratedFromRecurringSchedule() throws Exception {
        User user = userRepository.save(
                new User("Recurring protected", "recurring-protected@example.com", "password-hash")
        );
        Account account = accountRepository.save(
                new Account("Ví định kỳ", "CASH", BigDecimal.ZERO, user)
        );
        Category category = categoryRepository.save(
                new Category("Internet", TransactionType.EXPENSE, user)
        );
        recurringTransactionRepository.save(
                new RecurringTransaction(
                        account,
                        category,
                        BigDecimal.valueOf(200_000),
                        "Cước Internet",
                        RecurrenceFrequency.MONTHLY,
                        LocalDate.now()
                )
        );

        recurringTransactionService.runDueRecurringTransactions();

        Transaction transaction = transactionRepository.findByAccountId(account.getId()).getFirst();
        String token = jwtService.generateToken(user);

        mockMvc.perform(patch("/api/transactions/{id}", transaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 300000 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không thể chỉnh sửa hoặc xóa transaction được tạo tự động."));

        mockMvc.perform(delete("/api/transactions/{id}", transaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không thể chỉnh sửa hoặc xóa transaction được tạo tự động."));
    }

    @Test
    void archivingAccountDisablesRecurringTransactionsAndPreventsSchedulerFromCreatingTransactions() throws Exception {
        User user = userRepository.save(
                new User("Archived recurring", "archived-recurring@example.com", "password-hash")
        );
        Account account = accountRepository.save(
                new Account("Ví đã lưu trữ", "CASH", BigDecimal.ZERO, user)
        );
        Category category = categoryRepository.save(
                new Category("Điện", TransactionType.EXPENSE, user)
        );
        RecurringTransaction recurringTransaction = recurringTransactionRepository.save(
                new RecurringTransaction(
                        account,
                        category,
                        BigDecimal.valueOf(300_000),
                        "Tiền điện",
                        RecurrenceFrequency.MONTHLY,
                        LocalDate.now()
                )
        );

        mockMvc.perform(post("/api/accounts/{id}/archive", account.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isNoContent());

        RecurringTransaction archivedRecurring = recurringTransactionRepository
                .findById(recurringTransaction.getId())
                .orElseThrow();
        assertFalse(archivedRecurring.isActive());

        recurringTransactionService.runDueRecurringTransactions();

        assertTrue(transactionRepository.findByAccountId(account.getId()).isEmpty());
    }
}
