package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Budget;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.BudgetRepository;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.TransactionRepository;
import com.example.personalfinance.repository.UserRepository;
import com.example.personalfinance.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard-integration-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class DashboardIntegrationTest {
    private static final LocalDate MONTH = LocalDate.of(2026, 11, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private User userA;
    private User userB;
    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(new User("Dashboard A", "dashboard-a@example.com", "password-hash"));
        userB = userRepository.save(new User("Dashboard B", "dashboard-b@example.com", "password-hash"));

        Account mainAccount = accountRepository.save(
                new Account("Ví chính", "CASH", BigDecimal.valueOf(500_000), userA)
        );
        accountRepository.save(
                new Account("Ví phụ", "BANK", BigDecimal.valueOf(100_000), userA)
        );

        Category salary = categoryRepository.save(
                new Category("Lương", TransactionType.INCOME, userA)
        );
        Category food = categoryRepository.save(
                new Category("Ăn uống", TransactionType.EXPENSE, userA)
        );
        Category transport = categoryRepository.save(
                new Category("Di chuyển", TransactionType.EXPENSE, userA)
        );

        transactionRepository.saveAll(List.of(
                new Transaction(mainAccount, TransactionType.INCOME, BigDecimal.valueOf(150_000),
                        MONTH.atTime(9, 0), "Lương tháng", salary),
                new Transaction(mainAccount, TransactionType.EXPENSE, BigDecimal.valueOf(70_000),
                        MONTH.atTime(12, 0), "Ăn trưa", food),
                new Transaction(mainAccount, TransactionType.EXPENSE, BigDecimal.valueOf(50_000),
                        MONTH.withDayOfMonth(MONTH.lengthOfMonth()).atTime(23, 59), "Đi lại", transport),
                new Transaction(mainAccount, TransactionType.INCOME, BigDecimal.valueOf(999_000),
                        MONTH.plusMonths(1).atStartOfDay(), "Không thuộc tháng này", salary)
        ));

        budgetRepository.saveAll(List.of(
                new Budget(food, MONTH, BigDecimal.valueOf(100_000)),
                new Budget(transport, MONTH, BigDecimal.valueOf(40_000))
        ));

        userAToken = jwtService.generateToken(userA);
        userBToken = jwtService.generateToken(userB);
    }

    @Test
    void returnsMonthlyDashboardForCurrentUserOnly() throws Exception {
        mockMvc.perform(get("/api/dashboard/monthly")
                        .param("month", MONTH.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(MONTH.toString()))
                .andExpect(jsonPath("$.totalIncome").value(150_000))
                .andExpect(jsonPath("$.totalExpense").value(120_000))
                .andExpect(jsonPath("$.netChange").value(30_000))
                .andExpect(jsonPath("$.accountCount").value(2))
                .andExpect(jsonPath("$.budgetCount").value(2))
                .andExpect(jsonPath("$.overBudgetCount").value(1));
    }

    @Test
    void doesNotExposeAnotherUsersDashboardData() throws Exception {
        mockMvc.perform(get("/api/dashboard/monthly")
                        .param("month", MONTH.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.netChange").value(0))
                .andExpect(jsonPath("$.accountCount").value(0))
                .andExpect(jsonPath("$.budgetCount").value(0))
                .andExpect(jsonPath("$.overBudgetCount").value(0));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/monthly")
                        .param("month", MONTH.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDateThatIsNotFirstDayOfMonth() throws Exception {
        mockMvc.perform(get("/api/dashboard/monthly")
                        .param("month", "2026-11-02")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Phải là ngày đầu của tháng."));
    }
}
