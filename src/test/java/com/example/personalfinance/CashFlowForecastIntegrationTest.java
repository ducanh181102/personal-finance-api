package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtType;
import com.example.personalfinance.model.RecurrenceFrequency;
import com.example.personalfinance.model.RecurringTransaction;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.Transfer;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.DebtRepository;
import com.example.personalfinance.repository.RecurringTransactionRepository;
import com.example.personalfinance.repository.TransactionRepository;
import com.example.personalfinance.repository.TransferRepository;
import com.example.personalfinance.repository.UserRepository;
import com.example.personalfinance.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cash-flow-forecast-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CashFlowForecastIntegrationTest {
    private static final String PATH = "/api/reports/cash-flow-forecast";
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TransferRepository transferRepository;
    @Autowired private RecurringTransactionRepository recurringRepository;
    @Autowired private DebtRepository debtRepository;

    @Test
    void projectsOwnedCashFlowAndKeepsExistingTransactionsAndSchedulesUnchanged() throws Exception {
        LocalDate today = LocalDate.now(VIETNAM);
        User owner = createUser("forecast-owner");
        User other = createUser("forecast-other");
        Account main = accountRepository.save(new Account("Ví chính", "CASH", BigDecimal.valueOf(8_000_000), owner));
        Account archived = new Account("Ví cũ", "BANK", BigDecimal.valueOf(100_000), owner);
        archived.archive();
        archived = accountRepository.save(archived);
        Account otherAccount = accountRepository.save(new Account("Ví người khác", "BANK", BigDecimal.valueOf(50_000_000), other));

        Category expenses = categoryRepository.save(new Category("Tiền nhà", TransactionType.EXPENSE, owner));
        Category income = categoryRepository.save(new Category("Lương", TransactionType.INCOME, owner));
        Category otherIncome = categoryRepository.save(new Category("Thu riêng", TransactionType.INCOME, other));
        transactionRepository.save(new Transaction(archived, TransactionType.INCOME, BigDecimal.valueOf(500_000),
                today.minusDays(2).atTime(10, 0), "Đã nhận", income));
        transactionRepository.save(new Transaction(main, TransactionType.EXPENSE, BigDecimal.valueOf(600_000),
                today.minusDays(1).atTime(10, 0), "Đã chi", expenses));
        transactionRepository.save(new Transaction(otherAccount, TransactionType.INCOME, BigDecimal.valueOf(90_000_000),
                today.minusDays(1).atTime(10, 0), "Thu riêng", otherIncome));
        transferRepository.save(new Transfer(main, archived, BigDecimal.valueOf(200_000), today.minusDays(1).atStartOfDay(), null));

        RecurringTransaction overdueRecurring = saveSchedule(main, expenses, 200_000, today.minusDays(1), true);
        RecurringTransaction rent = saveSchedule(main, expenses, 5_000_000, today.plusDays(2), true);
        saveSchedule(main, expenses, 8_000_000, today.plusDays(1), false);
        saveSchedule(otherAccount, otherIncome, 7_000_000, today, true);

        Debt overdueDebt = new Debt(owner, "Khoản còn lại", DebtType.BORROWED, BigDecimal.valueOf(1_000_000),
                today.minusDays(2), null);
        overdueDebt.applyPayment(BigDecimal.valueOf(200_000));
        debtRepository.save(overdueDebt);
        saveDebt(owner, DebtType.BORROWED, 4_000_000, today.plusDays(4));
        saveDebt(owner, DebtType.LENT, 5_000_000, today.plusDays(5));
        saveDebt(owner, DebtType.LENT, 100_000, today.plusDays(9));
        saveDebt(owner, DebtType.BORROWED, 6_000_000, today.plusDays(10));
        saveDebt(other, DebtType.BORROWED, 40_000_000, today);
        Debt paid = new Debt(owner, "Đã tất toán", DebtType.BORROWED, BigDecimal.valueOf(500_000), today, null);
        paid.applyPayment(BigDecimal.valueOf(500_000));
        debtRepository.save(paid);
        long transactionsBefore = transactionRepository.count();

        mockMvc.perform(get(PATH).param("days", "10").header(HttpHeaders.AUTHORIZATION, token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(today.toString()))
                .andExpect(jsonPath("$.to").value(today.plusDays(9).toString()))
                .andExpect(jsonPath("$.startingBalance").value(8_000_000))
                .andExpect(jsonPath("$.endingBalance").value(3_100_000))
                .andExpect(jsonPath("$.firstShortfallDate").value(today.plusDays(4).toString()))
                .andExpect(jsonPath("$.days.length()").value(10))
                .andExpect(jsonPath("$.days[0].expense").value(1_000_000))
                .andExpect(jsonPath("$.days[0].projectedBalance").value(7_000_000))
                .andExpect(jsonPath("$.days[0].events.length()").value(2))
                .andExpect(jsonPath("$.days[0].events[0].source").value("DEBT"))
                .andExpect(jsonPath("$.days[0].events[0].amount").value(800_000))
                .andExpect(jsonPath("$.days[0].events[0].scheduledDate").value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$.days[0].events[1].sourceId").value(overdueRecurring.getId()))
                .andExpect(jsonPath("$.days[1].events.length()").value(0))
                .andExpect(jsonPath("$.days[2].events[0].source").value("RECURRING"))
                .andExpect(jsonPath("$.days[2].events[0].sourceId").value(rent.getId()))
                .andExpect(jsonPath("$.days[2].events[0].label").value("Tiền nhà"))
                .andExpect(jsonPath("$.days[4].projectedBalance").value(-2_000_000))
                .andExpect(jsonPath("$.days[5].income").value(5_000_000))
                .andExpect(jsonPath("$.days[9].income").value(100_000));

        assertEquals(transactionsBefore, transactionRepository.count());
        assertEquals(today.minusDays(1), recurringRepository.findById(overdueRecurring.getId()).orElseThrow().getNextRunDate());
    }

    @Test
    void projectsRepeatedOccurrencesUsingTheSchedulingRule() throws Exception {
        LocalDate today = LocalDate.now(VIETNAM);
        LocalDate first = today.plusDays(1);
        LocalDate second = first.plusMonths(1);
        User owner = createUser("forecast-repeats");
        Account account = accountRepository.save(new Account("Ví nhận", "CASH", BigDecimal.ZERO, owner));
        Category category = categoryRepository.save(new Category("Lương", TransactionType.INCOME, owner));
        saveSchedule(account, category, 500_000, first, true);
        String secondDay = "$.days[" + ChronoUnit.DAYS.between(today, second) + "]";

        mockMvc.perform(get(PATH).param("days", "50").header(HttpHeaders.AUTHORIZATION, token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[1].income").value(500_000))
                .andExpect(jsonPath(secondDay + ".date").value(second.toString()))
                .andExpect(jsonPath(secondDay + ".projectedBalance").value(1_000_000))
                .andExpect(jsonPath("$.endingBalance").value(1_000_000))
                .andExpect(jsonPath("$.firstShortfallDate").isEmpty());
    }

    @Test
    void defaultsToThirtyDaysAndValidatesTheHorizon() throws Exception {
        User owner = createUser("forecast-empty");
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(30))
                .andExpect(jsonPath("$.startingBalance").value(0))
                .andExpect(jsonPath("$.endingBalance").value(0))
                .andExpect(jsonPath("$.firstShortfallDate").isEmpty());
        for (String days : new String[]{"0", "366"}) {
            mockMvc.perform(get(PATH).param("days", days).header(HttpHeaders.AUTHORIZATION, token(owner)))
                    .andExpect(status().isBadRequest());
        }
    }

    private User createUser(String name) {
        return userRepository.save(new User(name, name + "@example.com", "password-hash"));
    }

    private String token(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private RecurringTransaction saveSchedule(Account account, Category category, long amount, LocalDate date, boolean active) {
        RecurringTransaction schedule = new RecurringTransaction(account, category, BigDecimal.valueOf(amount),
                null, RecurrenceFrequency.MONTHLY, date);
        schedule.setActive(active);
        return recurringRepository.save(schedule);
    }

    private void saveDebt(User owner, DebtType type, long amount, LocalDate dueDate) {
        debtRepository.save(new Debt(owner, "Khoản nợ", type, BigDecimal.valueOf(amount), dueDate, null));
    }
}
