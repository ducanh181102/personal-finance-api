package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.SavingsGoal;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.AccountRepository;
import com.example.personalfinance.repository.GoalContributionRepository;
import com.example.personalfinance.repository.SavingsGoalRepository;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:savings-goal-integration-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class SavingsGoalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    private Account account;
    private SavingsGoal goal;
    private String token;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("Goal User", "goal-user@example.com", "password-hash"));
        account = accountRepository.save(new Account("Ví góp", "CASH", BigDecimal.valueOf(1_000), user));
        goal = savingsGoalRepository.save(
                new SavingsGoal(user, "Mua laptop", BigDecimal.valueOf(200), LocalDate.of(2026, 12, 31))
        );
        token = jwtService.generateToken(user);
    }

    @Test
    void contributionUpdatesGoalAndDeletionReversesIt() throws Exception {
        mockMvc.perform(post("/api/goal-contributions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "savingsGoalId": %d,
                                  "accountId": %d,
                                  "amount": 100.00,
                                  "contributedAt": "2026-11-10T10:00:00",
                                  "note": "Góp lần đầu"
                                }
                                """.formatted(goal.getId(), account.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savingsGoalId").value(goal.getId()))
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.amount").value(100));

        mockMvc.perform(patch("/api/savings-goals/{id}", goal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAmount\": 50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(100))
                .andExpect(jsonPath("$.targetAmount").value(50))
                .andExpect(jsonPath("$.completed").value(true));

        int contributionId = goalContributionRepository.findAll().getFirst().getId();
        mockMvc.perform(delete("/api/goal-contributions/{id}", contributionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/savings-goals/{id}", goal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(0))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void preventsDeletingGoalThatHasContribution() throws Exception {
        mockMvc.perform(post("/api/goal-contributions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "savingsGoalId": %d,
                                  "accountId": %d,
                                  "amount": 100.00,
                                  "contributedAt": "2026-11-10T10:00:00"
                                }
                                """.formatted(goal.getId(), account.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/savings-goals/{id}", goal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không được xóa mục tiêu đã có khoản góp."));
    }

    @Test
    void validatesInvalidSavingsGoalRequest() throws Exception {
        mockMvc.perform(post("/api/savings-goals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "targetAmount": -1,
                                  "deadline": "2000-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.targetAmount").isNotEmpty())
                .andExpect(jsonPath("$.deadline").isNotEmpty());
    }
}
