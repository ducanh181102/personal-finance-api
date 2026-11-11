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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-integration-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    private String userAToken;
    private String userBToken;
    private Account accountA;
    private Category categoryA;
    private Transaction transactionA;
    private Budget budgetA;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(
                new User("User A", "security-a@example.com", "password-hash")
        );
        userA.setPasswordHash(passwordEncoder.encode("matkhau123"));
        userA = userRepository.save(userA);
        User userB = userRepository.save(
                new User("User B", "security-b@example.com", "password-hash")
        );

        accountA = accountRepository.save(
                new Account("Ví của A", "CASH", BigDecimal.valueOf(500_000), userA)
        );
        categoryA = categoryRepository.save(
                new Category("Ăn uống của A", TransactionType.EXPENSE, userA)
        );
        transactionA = transactionRepository.save(
                new Transaction(
                        accountA,
                        TransactionType.EXPENSE,
                        BigDecimal.valueOf(50_000),
                        LocalDateTime.of(2026, 11, 10, 12, 0),
                        "Bữa trưa",
                        categoryA
                )
        );
        budgetA = budgetRepository.save(
                new Budget(categoryA, LocalDate.of(2026, 11, 1), BigDecimal.valueOf(200_000))
        );

        userAToken = jwtService.generateToken(userA);
        userBToken = jwtService.generateToken(userB);
    }

    @Test
    void returns401WhenProtectedEndpointHasNoToken() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsOwnerToReadOwnAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountA.getId()));
    }

    @Test
    void preventsAnotherUserFromReadingResourcesById() throws Exception {
        String authorization = "Bearer " + userBToken;

        mockMvc.perform(get("/api/accounts/{id}", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/categories/{id}", categoryA.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions/{id}", transactionA.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/budgets/{id}", budgetA.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden());
    }

    @Test
    void hidesAnotherUsersTransactionsAndBudgetsInLists() throws Exception {
        String authorization = "Bearer " + userBToken;

        mockMvc.perform(get("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/transactions/page")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void preventsOwnerFromDeletingAccountThatHasTransactions() throws Exception {
        mockMvc.perform(delete("/api/accounts/{id}", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không được xóa tài khoản đã có giao dịch."));
    }

    @Test
    void allowsOwnerToDeleteAccountThatHasNoTransactions() throws Exception {
        Account emptyAccount = accountRepository.save(
                new Account("Ví trống", "CASH", BigDecimal.ZERO, userA)
        );

        mockMvc.perform(delete("/api/accounts/{id}", emptyAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isNoContent());

        assertFalse(accountRepository.existsById(emptyAccount.getId()));
    }

    @Test
    void rejectsOldTokenAfterUserIsDeactivated() throws Exception {
        String authorization = "Bearer " + userAToken;

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reactivatesUserAndReturnsTokenThatCanAccessProtectedApi() throws Exception {
        String oldAuthorization = "Bearer " + userAToken;

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, oldAuthorization))
                .andExpect(status().isNoContent());

        String responseBody = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/reactivate")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "email": "security-a@example.com",
                                          "oldPassword": "matkhau123"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newToken = responseBody.replaceFirst(
                ".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*",
                "$1"
        );

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken))
                .andExpect(status().isOk());
    }
}
