package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtPayment;
import com.example.personalfinance.model.DebtType;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionSource;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.DebtPaymentRepository;
import com.example.personalfinance.repository.DebtRepository;
import com.example.personalfinance.repository.UserRepository;
import com.example.personalfinance.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:debt-integration-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class DebtIntegrationTest {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 31);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private DebtPaymentRepository debtPaymentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User userA;
    private User userB;
    private Account accountA;
    private Account accountB;
    private Debt debtA;
    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(new User("Debt A", "debt-a@example.com", "password-hash"));
        userB = userRepository.save(new User("Debt B", "debt-b@example.com", "password-hash"));

        accountA = accountRepository.save(
                new Account("Ví của A", "CASH", BigDecimal.valueOf(2_000), userA)
        );
        accountB = accountRepository.save(
                new Account("Ví của B", "CASH", BigDecimal.valueOf(2_000), userB)
        );
        debtA = debtRepository.save(new Debt(
                userA,
                "Nam",
                DebtType.LENT,
                BigDecimal.valueOf(1_000),
                DUE_DATE,
                "Cho Nam mượn"
        ));

        userAToken = jwtService.generateToken(userA);
        userBToken = jwtService.generateToken(userB);
    }

    @Test
    void createsIncomeTransactionWithDebtRecoveryCategoryForLentDebt() throws Exception {
        createPayment(debtA.getId(), accountA.getId(), 300)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Transaction transaction = debtPaymentRepository.findAll().getFirst().getTransaction();
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals("THU HỒI NỢ", transaction.getCategory().getName());
        assertTrue(transaction.getCategory().isSystemCategory());
        assertEquals(4, categoryRepository.findByUserId(userA.getId()).size());
    }

    @Test
    void createsExpenseTransactionWithDebtRepaymentCategoryForBorrowedDebt() throws Exception {
        Debt borrowedDebt = debtRepository.save(new Debt(
                userA,
                "Minh",
                DebtType.BORROWED,
                BigDecimal.valueOf(500),
                DUE_DATE,
                "Mượn tiền"
        ));

        createPayment(borrowedDebt.getId(), accountA.getId(), 200)
                .andExpect(status().isCreated());

        Transaction transaction = debtPaymentRepository.findAll().getFirst().getTransaction();
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals("TRẢ NỢ", transaction.getCategory().getName());
        assertTrue(transaction.getCategory().isSystemCategory());
    }

    @Test
    void createsDebtWithFullRemainingAmountAndOpenStatus() throws Exception {
        mockMvc.perform(post("/api/debts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personName": "Lan",
                                  "type": "BORROWED",
                                  "originalAmount": 500.00,
                                  "dueDate": "2026-12-31",
                                  "note": "Vay để mua sách"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personName").value("Lan"))
                .andExpect(jsonPath("$.type").value("BORROWED"))
                .andExpect(jsonPath("$.originalAmount").value(500))
                .andExpect(jsonPath("$.remainingAmount").value(500))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createsMonthlyInstallmentPlanAndMovesNextDueDateAfterPayment() throws Exception {
        LocalDate firstInstallmentDate = LocalDate.now().plusDays(2);
        mockMvc.perform(post("/api/debts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personName": "Cửa hàng laptop",
                                  "type": "BORROWED",
                                  "originalAmount": 2500,
                                  "monthlyInstallmentAmount": 1000,
                                  "firstInstallmentDate": "%s"
                                }
                                """.formatted(firstInstallmentDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyInstallmentAmount").value(1000))
                .andExpect(jsonPath("$.firstInstallmentDate").value(firstInstallmentDate.toString()))
                .andExpect(jsonPath("$.nextInstallmentDate").value(firstInstallmentDate.toString()))
                .andExpect(jsonPath("$.nextInstallmentAmount").value(1000))
                .andExpect(jsonPath("$.installmentCount").value(3))
                .andExpect(jsonPath("$.dueDate").value(firstInstallmentDate.plusMonths(2).toString()));

        Debt installmentDebt = debtRepository.findAll().stream()
                .filter(debt -> debt.getPersonName().equals("Cửa hàng laptop"))
                .findFirst().orElseThrow();
        createPayment(installmentDebt.getId(), accountA.getId(), 1_000)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/debts/{id}", installmentDebt.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(1500))
                .andExpect(jsonPath("$.nextInstallmentDate").value(firstInstallmentDate.plusMonths(1).toString()))
                .andExpect(jsonPath("$.nextInstallmentAmount").value(1000));
    }

    @Test
    void keepsTheSameInstallmentDueDateUntilItsPartialPaymentIsCompleted() throws Exception {
        LocalDate firstInstallmentDate = LocalDate.now().plusDays(2);
        Debt debt = debtRepository.save(new Debt(userA, "Người cho vay", DebtType.BORROWED,
                new BigDecimal("2500"), firstInstallmentDate, null,
                new BigDecimal("1000"), firstInstallmentDate));

        debt.applyPayment(new BigDecimal("400"));
        debtRepository.save(debt);

        mockMvc.perform(get("/api/debts/{id}", debt.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextInstallmentDate").value(firstInstallmentDate.toString()))
                .andExpect(jsonPath("$.nextInstallmentAmount").value(600));
    }

    @Test
    void alertsUseTheNextInstallmentInsteadOfTheFinalDueDate() throws Exception {
        LocalDate firstInstallmentDate = LocalDate.now().plusDays(3);
        Debt installmentDebt = debtRepository.save(new Debt(
                userA, "Trả góp điện thoại", DebtType.BORROWED, BigDecimal.valueOf(900),
                firstInstallmentDate, null, BigDecimal.valueOf(300), firstInstallmentDate
        ));

        mockMvc.perform(get("/api/debts/alerts")
                        .param("days", "7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debtId").value(installmentDebt.getId()))
                .andExpect(jsonPath("$[0].dueDate").value(firstInstallmentDate.toString()))
                .andExpect(jsonPath("$[0].scheduledAmount").value(300))
                .andExpect(jsonPath("$[0].installment").value(true));
    }

    @Test
    void paymentReducesDebtAndMarksPaidWhenFullyPaid() throws Exception {
        createPayment(debtA.getId(), accountA.getId(), 300)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/debts/{id}", debtA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(700))
                .andExpect(jsonPath("$.status").value("OPEN"));

        createPayment(debtA.getId(), accountA.getId(), 700)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/debts/{id}", debtA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(0))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void rejectsPaymentGreaterThanRemainingAmount() throws Exception {
        createPayment(debtA.getId(), accountA.getId(), 1_001)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Số tiền thanh toán vượt quá số nợ còn lại."));
    }

    @Test
    void cancellingPaymentKeepsOriginalTransactionAndCreatesReversalTransaction() throws Exception {
        createPayment(debtA.getId(), accountA.getId(), 1_000)
                .andExpect(status().isCreated());

        int paymentId = debtPaymentRepository.findAll().getFirst().getId();
        mockMvc.perform(post("/api/debt-payments/{id}/cancel", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.transactionId").isNumber())
                .andExpect(jsonPath("$.reversalTransactionId").isNumber());

        DebtPayment payment = debtPaymentRepository.findById(paymentId).orElseThrow();
        assertNotNull(payment.getTransaction());
        assertNotNull(payment.getReversalTransaction());
        assertNotNull(payment.getCancelledAt());
        assertEquals(TransactionType.INCOME, payment.getTransaction().getType());
        assertEquals(TransactionType.EXPENSE, payment.getReversalTransaction().getType());
        assertEquals(TransactionSource.DEBT_PAYMENT, payment.getTransaction().getSource());
        assertEquals(TransactionSource.DEBT_PAYMENT_REVERSAL, payment.getReversalTransaction().getSource());
        assertEquals("HOÀN THU HỒI NỢ", payment.getReversalTransaction().getCategory().getName());

        mockMvc.perform(get("/api/debts/{id}", debtA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(1_000))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void preventsUpdatingOrDeletingTransactionGeneratedFromDebtPayment() throws Exception {
        createPayment(debtA.getId(), accountA.getId(), 100)
                .andExpect(status().isCreated());

        int transactionId = debtPaymentRepository.findAll().getFirst().getTransaction().getId();

        mockMvc.perform(patch("/api/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 200 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không thể chỉnh sửa hoặc xóa transaction được tạo tự động."));

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không thể chỉnh sửa hoặc xóa transaction được tạo tự động."));
    }

    @Test
    void preventsDeletingDebtThatHasPayment() throws Exception {
        createPayment(debtA.getId(), accountA.getId(), 100)
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/debts/{id}", debtA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không thể xoá nợ đã có thanh toán."));
    }

    @Test
    void preventsAnotherUserFromAccessingDebtOrCreatingPaymentForIt() throws Exception {
        mockMvc.perform(get("/api/debts/{id}", debtA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        createPaymentAsUser(userBToken, debtA.getId(), accountB.getId(), 100)
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsOnlyCurrentUsersOpenDebtsThatAreOverdueOrDueSoon() throws Exception {
        LocalDate today = LocalDate.now();
        debtRepository.save(new Debt(
                userA, "Quá hạn", DebtType.BORROWED, BigDecimal.valueOf(100),
                today.minusDays(2), "Nợ quá hạn"
        ));
        debtRepository.save(new Debt(
                userA, "Sắp hạn", DebtType.LENT, BigDecimal.valueOf(200),
                today.plusDays(3), "Nợ sắp đến hạn"
        ));
        debtRepository.save(new Debt(
                userA, "Còn xa", DebtType.BORROWED, BigDecimal.valueOf(300),
                today.plusDays(8), "Chưa cần cảnh báo"
        ));

        Debt paidDebt = debtRepository.save(new Debt(
                userA, "Đã trả", DebtType.LENT, BigDecimal.valueOf(400),
                today.plusDays(1), "Đã hoàn tất"
        ));
        paidDebt.applyPayment(BigDecimal.valueOf(400));
        debtRepository.save(paidDebt);

        debtRepository.save(new Debt(
                userB, "User khác", DebtType.BORROWED, BigDecimal.valueOf(500),
                today.plusDays(1), "Không được nhìn thấy"
        ));

        mockMvc.perform(get("/api/debts/alerts")
                        .param("days", "7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].personName").value("Quá hạn"))
                .andExpect(jsonPath("$[0].overdue").value(true))
                .andExpect(jsonPath("$[0].daysUntilDue").value(-2))
                .andExpect(jsonPath("$[1].personName").value("Sắp hạn"))
                .andExpect(jsonPath("$[1].overdue").value(false))
                .andExpect(jsonPath("$[1].daysUntilDue").value(3));
    }

    private org.springframework.test.web.servlet.ResultActions createPayment(int debtId,
                                                                             int accountId,
                                                                             int amount) throws Exception {
        return createPaymentAsUser(userAToken, debtId, accountId, amount);
    }

    private org.springframework.test.web.servlet.ResultActions createPaymentAsUser(String token,
                                                                                   int debtId,
                                                                                   int accountId,
                                                                                   int amount) throws Exception {
        return mockMvc.perform(post("/api/debt-payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "debtId": %d,
                          "accountId": %d,
                          "amount": %d,
                          "paidAt": "2026-11-10T10:00:00",
                          "note": "Thanh toán nợ"
                        }
                        """.formatted(debtId, accountId, amount)));
    }

}
