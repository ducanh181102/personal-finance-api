package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Transfer;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.TransferRepository;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transfer-integration-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class TransferIntegrationTest {

    private static final LocalDateTime TRANSFER_TIME = LocalDateTime.of(2026, 11, 10, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    private User userA;
    private Account fromAccount;
    private Account toAccount;
    private Account userBAccount;
    private String userAToken;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(new User("Transfer A", "transfer-a@example.com", "password-hash"));
        User userB = userRepository.save(new User("Transfer B", "transfer-b@example.com", "password-hash"));

        fromAccount = accountRepository.save(
                new Account("Ví gửi", "CASH", BigDecimal.valueOf(1_000), userA)
        );
        toAccount = accountRepository.save(
                new Account("Ví nhận", "BANK", BigDecimal.valueOf(500), userA)
        );
        userBAccount = accountRepository.save(
                new Account("Ví của B", "CASH", BigDecimal.valueOf(300), userB)
        );
        userAToken = jwtService.generateToken(userA);
    }

    @Test
    void createsTransferAndCalculatesBothAccountBalances() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": %d,
                                  "toAccountId": %d,
                                  "amount": 200.00,
                                  "transferDate": "2026-11-10T10:00:00",
                                  "note": "Chuyển sang ngân hàng"
                                }
                                """.formatted(fromAccount.getId(), toAccount.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromAccountId").value(fromAccount.getId()))
                .andExpect(jsonPath("$.toAccountId").value(toAccount.getId()))
                .andExpect(jsonPath("$.amount").value(200))
                .andExpect(jsonPath("$.fromAccount").doesNotExist())
                .andExpect(jsonPath("$.toAccount").doesNotExist());

        mockMvc.perform(get("/api/accounts/{id}/balance", fromAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(800));

        mockMvc.perform(get("/api/accounts/{id}/balance", toAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(700));

        mockMvc.perform(get("/api/accounts/{id}/summary", fromAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.currentBalance").value(800));
    }

    @Test
    void preventsTransferToAnotherUsersAccount() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": %d,
                                  "toAccountId": %d,
                                  "amount": 200.00,
                                  "transferDate": "2026-11-10T10:00:00"
                                }
                                """.formatted(fromAccount.getId(), userBAccount.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Không có quyền truy cập."));
    }

    @Test
    void preventsDeletingAccountThatHasTransfer() throws Exception {
        transferRepository.save(new Transfer(
                fromAccount,
                toAccount,
                BigDecimal.valueOf(200),
                TRANSFER_TIME,
                "Đã chuyển"
        ));

        mockMvc.perform(delete("/api/accounts/{id}", fromAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không được xóa tài khoản đã có giao dịch chuyển tiền."));
    }
}
