package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Budget;
import com.example.personalfinance.model.Category;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtType;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.AppNotificationRepository;
import com.example.personalfinance.repository.BudgetRepository;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.DebtRepository;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-integration-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class NotificationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BudgetRepository budgetRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private DebtRepository debtRepository;
    @Autowired
    private AppNotificationRepository notificationRepository;
    @Autowired
    private JwtService jwtService;

    private User user;
    private String authorization;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("Người dùng thông báo", "notification@example.com", "password-hash"));
        authorization = "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void createsCurrentBudgetAndDebtNotificationsOnlyOnceAndMarksOneAsRead() throws Exception {
        LocalDate today = LocalDate.now();
        Account account = accountRepository.save(new Account("Ví thông báo", "CASH", BigDecimal.ZERO, user));
        Category category = categoryRepository.save(new Category("Ăn uống", TransactionType.EXPENSE, user));
        budgetRepository.save(new Budget(category, today.withDayOfMonth(1), BigDecimal.valueOf(100_000)));
        transactionRepository.save(new Transaction(
                account, TransactionType.EXPENSE, BigDecimal.valueOf(120_000), today.atStartOfDay(), "Chi vượt ngân sách", category
        ));
        debtRepository.save(new Debt(
                user, "Nguyễn An", DebtType.BORROWED, BigDecimal.valueOf(500_000), today.plusDays(1), "Khoản sắp đến hạn"
        ));

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.notifications.length()").value(2))
                .andExpect(jsonPath("$.notifications[*].type", containsInAnyOrder("BUDGET_EXCEEDED", "DEBT_DUE")));

        // Mở lại trang không được tạo thêm cùng một cảnh báo.
        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(2));
        assertEquals(2, notificationRepository.count());

        int notificationId = notificationRepository.findAll().getFirst().getId();
        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }
}
