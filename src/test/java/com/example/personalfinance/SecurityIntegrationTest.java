package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.AccountType;
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
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-integration-test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.auth.secure-cookies=true",
        "app.password-reset.from="
})
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    private static final String SESSION_COOKIE = "__Host-finance-session";
    private static final String CSRF_COOKIE = "__Host-finance-csrf";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String LOGIN_BODY = """
            {"email":"security-a@example.com","password":"matkhau123"}
            """;

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
    void registersUserBeforeEmailVerification() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New user","email":"signup@example.test","password":"signup-password-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("signup@example.test"))
                .andExpect(jsonPath("$.emailVerifiedAt").value(nullValue()));
    }

    @Test
    void returnsVerificationTimestampInUtc() throws Exception {
        Instant verifiedAt = Instant.parse("2026-11-20T02:30:00Z");
        userA.setEmailVerifiedAt(verifiedAt);
        userRepository.save(userA);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerifiedAt").value(verifiedAt.toString()));
    }

    @Test
    void browserSessionUsesSecureCookieAndRequiresCsrfForLoginWritesAndLogout() throws Exception {
        mockMvc.perform(post("/api/auth/session")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isForbidden());

        Cookie csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().secure(CSRF_COOKIE, true))
                .andExpect(cookie().httpOnly(CSRF_COOKIE, false))
                .andReturn().getResponse().getCookie(CSRF_COOKIE);
        Cookie session = mockMvc.perform(post("/api/auth/session")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userA.getEmail()))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(cookie().httpOnly(SESSION_COOKIE, true))
                .andExpect(cookie().secure(SESSION_COOKIE, true))
                .andExpect(cookie().path(SESSION_COOKIE, "/"))
                .andExpect(cookie().sameSite(SESSION_COOKIE, "Lax"))
                .andReturn().getResponse().getCookie(SESSION_COOKIE);

        mockMvc.perform(get("/api/users/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userA.getId()));

        String profileChange = """
                {"name":"User A updated"}
                """;
        mockMvc.perform(patch("/api/users/me").cookie(session, csrf)
                        .contentType(MediaType.APPLICATION_JSON).content(profileChange))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/users/me").cookie(session, csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(profileChange))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User A updated"));

        mockMvc.perform(delete("/api/auth/session").cookie(session, csrf))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/auth/session").cookie(session, csrf).header(CSRF_HEADER, csrf.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(SESSION_COOKIE, 0))
                .andExpect(cookie().secure(SESSION_COOKIE, true));
    }

    @Test
    void requestingVerificationKeepsBrowserSession() throws Exception {
        Cookie csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie(CSRF_COOKIE);
        Cookie session = new Cookie(SESSION_COOKIE, userAToken);

        mockMvc.perform(post("/api/auth/request-verify-email")
                        .cookie(session, csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"security-a@example.com"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist(SESSION_COOKIE));

        mockMvc.perform(get("/api/users/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userA.getEmail()));
    }

    @Test
    void bearerRequestsTakePrecedenceOverBrowserCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().doesNotExist(SESSION_COOKIE));

        Cookie session = new Cookie(SESSION_COOKIE, userAToken);
        mockMvc.perform(get("/api/users/me")
                        .cookie(session).header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("security-b@example.com"));
        mockMvc.perform(patch("/api/users/me")
                        .cookie(session).header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Invalid change\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsOwnerToReadOwnAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountA.getId()));
    }

    @ParameterizedTest
    @EnumSource(AccountType.class)
    void acceptsDefinedAccountTypes(AccountType type) throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ví mới","type":"%s","openingBalance":0}
                                """.formatted(type.name())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value(type.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"Tự nhập\"", "0", "\"0\"", "\"\"", "null", "true", "[\"CASH\"]"})
    void rejectsUndefinedAccountTypes(String type) throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ví mới","type":%s,"openingBalance":0}
                                """.formatted(type)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsStoredTypeForAccountCreatedBeforeTypeWasRestricted() throws Exception {
        Account legacyAccount = accountRepository.save(
                new Account("Ví cũ", "Thẻ nội địa", BigDecimal.ZERO, userA)
        );

        mockMvc.perform(get("/api/accounts/{id}", legacyAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Thẻ nội địa"));
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
    void filtersTransactionsAcrossOwnAccountsIncludingBothDayBoundaries() throws Exception {
        LocalDate day = transactionA.getTransactionDate().toLocalDate();
        Account secondAccount = accountRepository.save(new Account("Ví thứ hai của A", "CASH", BigDecimal.ZERO, userA));
        Transaction atStart = transactionRepository.save(new Transaction(
                accountA, TransactionType.EXPENSE, BigDecimal.TEN, day.atStartOfDay(), "Đầu ngày", categoryA));
        Transaction atEnd = transactionRepository.save(new Transaction(
                secondAccount, TransactionType.EXPENSE, BigDecimal.TEN, day.atTime(23, 59, 59), "Cuối ngày", categoryA));
        transactionRepository.save(new Transaction(
                accountA, TransactionType.EXPENSE, BigDecimal.TEN, day.minusDays(1).atTime(23, 59, 59), "Ngày trước", categoryA));
        transactionRepository.save(new Transaction(
                secondAccount, TransactionType.EXPENSE, BigDecimal.TEN, day.plusDays(1).atStartOfDay(), "Ngày sau", categoryA));

        mockMvc.perform(get("/api/transactions/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .param("from", day.toString()).param("to", day.toString())
                        .param("sort", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").value(atStart.getId()))
                .andExpect(jsonPath("$.content[1].id").value(transactionA.getId()))
                .andExpect(jsonPath("$.content[2].id").value(atEnd.getId()));

        mockMvc.perform(get("/api/transactions/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .param("accountId", secondAccount.getId().toString())
                        .param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(atEnd.getId()));

        mockMvc.perform(get("/api/transactions/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
                        .param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsAnIncompleteTransactionDateRange() throws Exception {
        mockMvc.perform(get("/api/transactions/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .param("from", "2026-11-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cần gửi cả ngày bắt đầu và ngày kết thúc."));
    }

    @Test
    void rejectsAReversedTransactionDateRange() throws Exception {
        mockMvc.perform(get("/api/transactions/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .param("from", "2026-11-11").param("to", "2026-11-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ngày bắt đầu không được sau ngày kết thúc."));
    }

    @Test
    void rejectsAReversedTransactionAmountRange() throws Exception {
        mockMvc.perform(get("/api/transactions/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .param("minAmount", "100000").param("maxAmount", "50000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Số tiền tối thiểu không được lớn hơn số tiền tối đa."));
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
    void archivesAccountBlocksNewTransactionAndCanBeActivatedAgain() throws Exception {
        mockMvc.perform(post("/api/accounts/{id}/archive", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/{id}", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "categoryId": %d,
                                  "type": "EXPENSE",
                                  "amount": 10000,
                                  "transactionDate": "2026-11-10T12:00:00"
                                }
                                """.formatted(accountA.getId(), categoryA.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không thể thực hiện giao dịch với tài khoản đã lưu trữ."));

        mockMvc.perform(post("/api/accounts/{id}/activate", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "categoryId": %d,
                                  "type": "EXPENSE",
                                  "amount": 10000,
                                  "transactionDate": "2026-11-10T12:00:00"
                                }
                                """.formatted(accountA.getId(), categoryA.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void hidesArchivedAccountsByDefaultAndReturnsThemWhenRequested() throws Exception {
        mockMvc.perform(post("/api/accounts/{id}/archive", accountA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/accounts")
                        .param("includeArchived", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(accountA.getId()))
                .andExpect(jsonPath("$[0].status").value("ARCHIVED"));
    }

    @Test
    void archivesCategoryBlocksNewFinancialOperationsAndCanBeActivatedAgain() throws Exception {
        mockMvc.perform(post("/api/categories/{id}/archive", categoryA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/{id}", categoryA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "categoryId": %d,
                                  "type": "EXPENSE",
                                  "amount": 10000,
                                  "transactionDate": "2026-11-10T12:00:00"
                                }
                                """.formatted(accountA.getId(), categoryA.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể sử dụng danh mục đã lưu trữ."));

        mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "month": "2026-12-01",
                                  "limitAmount": 200000
                                }
                                """.formatted(categoryA.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể sử dụng danh mục đã lưu trữ."));

        mockMvc.perform(post("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "categoryId": %d,
                                  "amount": 10000,
                                  "frequency": "MONTHLY",
                                  "nextRunDate": "%s"
                                }
                                """.formatted(accountA.getId(), categoryA.getId(), LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể sử dụng danh mục đã lưu trữ."));

        mockMvc.perform(post("/api/categories/{id}/activate", categoryA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "categoryId": %d,
                                  "type": "EXPENSE",
                                  "amount": 10000,
                                  "transactionDate": "2026-11-10T12:00:00"
                                }
                                """.formatted(accountA.getId(), categoryA.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void preventsArchivingSystemCategory() throws Exception {
        Category systemCategory = categoryRepository.save(
                new Category("TRẢ NỢ", TransactionType.EXPENSE, userA, true)
        );

        mockMvc.perform(post("/api/categories/{id}/archive", systemCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể lưu trữ danh mục hệ thống."));
    }

    @Test
    void rejectsManualTransactionWithSystemCategory() throws Exception {
        Category systemCategory = categoryRepository.save(
                new Category("TRẢ NỢ", TransactionType.EXPENSE, userA, true)
        );
        String authorization = "Bearer " + userAToken;

        mockMvc.perform(get("/api/categories/{id}", systemCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemCategory").value(true));

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "categoryId": %d,
                                  "type": "EXPENSE",
                                  "amount": 10000,
                                  "transactionDate": "%s"
                                }
                                """.formatted(accountA.getId(), systemCategory.getId(), LocalDate.now().minusDays(1).atTime(12, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể dùng danh mục hệ thống cho giao dịch nhập tay."));

        assertFalse(transactionRepository.existsByCategoryId(systemCategory.getId()));
    }

    @Test
    void updatesDisplayNameAndKeepsCurrentSession() throws Exception {
        String authorization = "Bearer " + userAToken;
        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Tên hiển thị mới  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tên hiển thị mới"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tên hiển thị mới"));
    }

    @Test
    void changesLoginEmailAndClearsBrowserSession() throws Exception {
        userA.markEmailVerified(Instant.parse("2026-11-20T02:30:00Z"));
        userRepository.save(userA);
        Cookie session = new Cookie(SESSION_COOKIE, userAToken);
        Cookie csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie(CSRF_COOKIE);

        mockMvc.perform(patch("/api/users/me")
                        .cookie(session, csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new-address@example.test","currentPassword":"matkhau123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new-address@example.test"))
                .andExpect(jsonPath("$.emailVerifiedAt").value(nullValue()))
                .andExpect(cookie().maxAge(SESSION_COOKIE, 0));

        mockMvc.perform(get("/api/users/me").cookie(session))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/session")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new-address@example.test","password":"matkhau123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userA.getId()));
    }

    @Test
    void requiresCurrentPasswordForCredentialChangesThroughProfilePatch() throws Exception {
        String authorization = "Bearer " + userAToken;
        for (String payload : new String[] {
                "{\"password\":\"replacement-test-password\"}",
                "{\"email\":\"other@example.test\"}",
                "{\"name\":\"Changed\",\"password\":\"replacement-test-password\",\"currentPassword\":\"wrong\"}"
        }) {
            mockMvc.perform(patch("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, authorization)
                            .contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User A"))
                .andExpect(jsonPath("$.email").value("security-a@example.com"));
    }

    @Test
    void validatesCurrentPasswordAndRevokesOldSessionOnPasswordChange() throws Exception {
        String authorization = "Bearer " + userAToken;
        String validChange = "{\"currentPassword\":\"matkhau123\",\"newPassword\":\"replacement-test-password\"}";
        mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON).content(validChange))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange.replace("matkhau123", "incorrect-password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu hiện tại không đúng."));

        mockMvc.perform(patch("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(validChange))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized());
        for (String password : new String[] { "matkhau123", "replacement-test-password" }) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"security-a@example.com","password":"%s"}
                                    """.formatted(password)))
                    .andExpect(status().is(password.equals("matkhau123") ? 401 : 200));
        }
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
