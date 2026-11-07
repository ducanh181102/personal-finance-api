package com.example.personalfinance;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Transaction;
import com.example.personalfinance.model.TransactionType;
import com.example.personalfinance.repository.AccountRepository;
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
        "spring.datasource.url=jdbc:h2:mem:account-period-summary-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class AccountPeriodSummaryTest {
    private static final LocalDate REPORT_DATE = LocalDate.of(2026, 11, 7);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account account;

    @BeforeEach
    void createAccount() {
        account = accountRepository.save(new Account("Ví test", "CASH", BigDecimal.valueOf(500_000)));
    }

    @Test
    void returnsIncomeExpenseAndNetChangeForTheRequestedDay() throws Exception {
        transactionRepository.saveAll(List.of(
                new Transaction(account, TransactionType.INCOME, BigDecimal.valueOf(100_000),
                        REPORT_DATE.atStartOfDay(), "Thu đầu ngày"),
                new Transaction(account, TransactionType.EXPENSE, BigDecimal.valueOf(45_000),
                        REPORT_DATE.atTime(23, 59, 59), "Chi cuối ngày"),
                new Transaction(account, TransactionType.INCOME, BigDecimal.valueOf(20_000),
                        REPORT_DATE.plusDays(1).atStartOfDay(), "Thu ngày hôm sau")
        ));

        mockMvc.perform(get("/api/accounts/{id}/period-summary", account.getId())
                        .param("from", REPORT_DATE.toString())
                        .param("to", REPORT_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.from").value(REPORT_DATE.toString()))
                .andExpect(jsonPath("$.to").value(REPORT_DATE.toString()))
                .andExpect(jsonPath("$.totalIncome").value(100_000))
                .andExpect(jsonPath("$.totalExpense").value(45_000))
                .andExpect(jsonPath("$.netChange").value(55_000));
    }

    @Test
    void rejectsAStartDateAfterTheEndDate() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}/period-summary", account.getId())
                        .param("from", REPORT_DATE.plusDays(1).toString())
                        .param("to", REPORT_DATE.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ngày bắt đầu không được sau ngày kết thúc"));
    }
}
