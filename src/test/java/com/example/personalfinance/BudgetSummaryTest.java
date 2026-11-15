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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:budget-summary-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class BudgetSummaryTest {
    private static final LocalDate BUDGET_MONTH = LocalDate.of(2026, 11, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User user;
    private Account account;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("User kiểm thử", "budget-summary@example.com", "password-hash"));
        account = accountRepository.save(
                new Account("Ví test budget", "CASH", BigDecimal.valueOf(500_000), user)
        );
        expenseCategory = categoryRepository.save(
                new Category("Ăn uống", TransactionType.EXPENSE, user)
        );
    }

    @Test
    void calculatesBudgetSummaryAndExcludesTransactionsFromTheNextMonth() throws Exception {
        Budget budget = budgetRepository.save(
                new Budget(expenseCategory, BUDGET_MONTH, BigDecimal.valueOf(100_000))
        );

        transactionRepository.saveAll(List.of(
                new Transaction(account, TransactionType.EXPENSE, BigDecimal.valueOf(50_000),
                        BUDGET_MONTH.atTime(12, 0), "Chi đầu tháng", expenseCategory),
                new Transaction(account, TransactionType.EXPENSE, BigDecimal.valueOf(70_000),
                        BUDGET_MONTH.withDayOfMonth(BUDGET_MONTH.lengthOfMonth()).atTime(23, 59, 59),
                        "Chi cuối tháng", expenseCategory),
                new Transaction(account, TransactionType.EXPENSE, BigDecimal.valueOf(99_000),
                        BUDGET_MONTH.plusMonths(1).atStartOfDay(),
                        "Chi đầu tháng sau", expenseCategory)
        ));

        mockMvc.perform(get("/api/budgets/{id}/summary", budget.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").value(budget.getId()))
                .andExpect(jsonPath("$.month").value(BUDGET_MONTH.toString()))
                .andExpect(jsonPath("$.limitAmount").value(100_000))
                .andExpect(jsonPath("$.spentAmount").value(120_000))
                .andExpect(jsonPath("$.remainingAmount").value(-20_000))
                .andExpect(jsonPath("$.usagePercentage").value(120.00))
                .andExpect(jsonPath("$.overBudget").value(true))
                .andExpect(jsonPath("$.status").value("EXCEEDED"));
    }

    @Test
    void returnsWarningWhenUsageIsAtLeastEightyPercent() throws Exception {
        Budget budget = createBudgetWithExpense("Đi lại", 80_000);

        mockMvc.perform(get("/api/budgets/{id}/summary", budget.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usagePercentage").value(80.00))
                .andExpect(jsonPath("$.overBudget").value(false))
                .andExpect(jsonPath("$.status").value("WARNING"));
    }

    @Test
    void returnsSafeWhenUsageIsBelowEightyPercent() throws Exception {
        Budget budget = createBudgetWithExpense("Mua sắm", 79_000);

        mockMvc.perform(get("/api/budgets/{id}/summary", budget.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usagePercentage").value(79.00))
                .andExpect(jsonPath("$.status").value("SAFE"));
    }

    @Test
    void returnsOnlyWarningAndExceededBudgetsAsAlerts() throws Exception {
        createBudgetWithExpense("An toàn", 50_000);
        createBudgetWithExpense("Cảnh báo", 80_000);
        createBudgetWithExpense("Vượt mức", 101_000);

        mockMvc.perform(get("/api/budgets/alerts")
                        .param("month", BUDGET_MONTH.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].status", containsInAnyOrder("WARNING", "EXCEEDED")));
    }

    @Test
    void copiesPreviousMonthBudgetsToTheTargetMonth() throws Exception {
        LocalDate previousMonth = BUDGET_MONTH.minusMonths(1);
        budgetRepository.save(new Budget(expenseCategory, previousMonth, BigDecimal.valueOf(100_000)));

        Category transportCategory = categoryRepository.save(
                new Category("Di chuyển", TransactionType.EXPENSE, user)
        );
        budgetRepository.save(new Budget(transportCategory, previousMonth, BigDecimal.valueOf(200_000)));

        mockMvc.perform(post("/api/budgets/copy-from-previous-month")
                        .param("targetMonth", BUDGET_MONTH.withDayOfMonth(15).toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].month", containsInAnyOrder(
                        BUDGET_MONTH.toString(), BUDGET_MONTH.toString()
                )))
                .andExpect(jsonPath("$[*].limitAmount", containsInAnyOrder(100_000, 200_000)));

        List<Budget> targetBudgets = budgetRepository.findByCategoryUserIdAndMonth(
                user.getId(), BUDGET_MONTH
        );
        org.junit.jupiter.api.Assertions.assertEquals(2, targetBudgets.size());
    }

    @Test
    void returnsEmptyListWhenPreviousMonthHasNoBudgetsToCopy() throws Exception {
        mockMvc.perform(post("/api/budgets/copy-from-previous-month")
                        .param("targetMonth", BUDGET_MONTH.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        org.junit.jupiter.api.Assertions.assertTrue(
                budgetRepository.findByCategoryUserIdAndMonth(user.getId(), BUDGET_MONTH).isEmpty()
        );
    }

    @Test
    void doesNotCopyBudgetWhenItsCategoryAlreadyHasTargetMonthBudget() throws Exception {
        LocalDate previousMonth = BUDGET_MONTH.minusMonths(1);
        budgetRepository.save(new Budget(expenseCategory, previousMonth, BigDecimal.valueOf(100_000)));
        budgetRepository.save(new Budget(expenseCategory, BUDGET_MONTH, BigDecimal.valueOf(300_000)));

        mockMvc.perform(post("/api/budgets/copy-from-previous-month")
                        .param("targetMonth", BUDGET_MONTH.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        List<Budget> targetBudgets = budgetRepository.findByCategoryUserIdAndMonth(
                user.getId(), BUDGET_MONTH
        );
        org.junit.jupiter.api.Assertions.assertEquals(1, targetBudgets.size());
        org.junit.jupiter.api.Assertions.assertEquals(
                BigDecimal.valueOf(300_000), targetBudgets.getFirst().getLimitAmount()
        );
    }

    private Budget createBudgetWithExpense(String categoryName, int amount) {
        Category category = categoryRepository.save(
                new Category(categoryName, TransactionType.EXPENSE, user)
        );
        Budget budget = budgetRepository.save(
                new Budget(category, BUDGET_MONTH, BigDecimal.valueOf(100_000))
        );
        transactionRepository.save(new Transaction(
                account,
                TransactionType.EXPENSE,
                BigDecimal.valueOf(amount),
                BUDGET_MONTH.atTime(12, 0),
                "Chi tiêu kiểm thử",
                category
        ));
        return budget;
    }
}
