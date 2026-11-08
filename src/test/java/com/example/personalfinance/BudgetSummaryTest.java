package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Budget;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.BudgetRepository;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private Account account;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        account = accountRepository.save(
                new Account("Ví test budget", "CASH", BigDecimal.valueOf(500_000))
        );
        expenseCategory = categoryRepository.save(
                new Category("Ăn uống", TransactionType.EXPENSE)
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

        mockMvc.perform(get("/api/budgets/{id}/summary", budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").value(budget.getId()))
                .andExpect(jsonPath("$.month").value(BUDGET_MONTH.toString()))
                .andExpect(jsonPath("$.limitAmount").value(100_000))
                .andExpect(jsonPath("$.spentAmount").value(120_000))
                .andExpect(jsonPath("$.remainingAmount").value(-20_000))
                .andExpect(jsonPath("$.usagePercentage").value(120.00))
                .andExpect(jsonPath("$.overBudget").value(true));
    }
}
