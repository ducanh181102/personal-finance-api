package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtPayment;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.DebtPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DebtPaymentService {
    private final DebtPaymentRepository debtPaymentRepository;
    private final CurrentUserService currentUserService;
    private final DebtService debtService;
    private final AccountService accountService;

    public DebtPaymentService(DebtPaymentRepository debtPaymentRepository, CurrentUserService currentUserService, DebtService debtService, AccountService accountService) {
        this.debtPaymentRepository = debtPaymentRepository;
        this.currentUserService = currentUserService;
        this.debtService = debtService;
        this.accountService = accountService;
    }

    private DebtPayment getOwnedDebtPaymentById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<DebtPayment> debtPayment = debtPaymentRepository.findById(id);
        if (debtPayment.isEmpty()) throw new NotFoundException(id);
        if (debtPayment.get().getAccount().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return debtPayment.get();
    }

    public List<DebtPayment> getAllDebtPayments() {
        return debtPaymentRepository.findByAccountUserId(currentUserService.getCurrentUser().getId());
    }

    public DebtPayment getDebtPaymentById(int id) {
        return getOwnedDebtPaymentById(id);
    }

    @Transactional
    public DebtPayment createDebtPayment(Integer debtId,
                                         Integer accountId,
                                         BigDecimal amount,
                                         LocalDateTime paidAt,
                                         String note) {

        Debt debt = debtService.getDebtById(debtId);
        Account account = accountService.getAccountById(accountId);

        DebtPayment debtPayment = new DebtPayment(debt, account, amount, paidAt, note);
        // Cập nhật debt
        debt.applyPayment(amount);

        return debtPaymentRepository.save(debtPayment);
    }

    @Transactional
    public void deleteDebtPaymentById(int id) {
        DebtPayment debtPayment = getDebtPaymentById(id);
        debtPaymentRepository.delete(debtPayment);
        // Cập nhật debt
        Debt debt = debtService.getDebtById(debtPayment.getDebt().getId());
        debt.reversePayment(debtPayment.getAmount());
    }

    public boolean isExistByDebtId(int debtId) {
        return debtPaymentRepository.existsByDebtId(debtId);
    }

    public boolean isExistByAccountId(int accountId) {
        return debtPaymentRepository.existsByAccountId(accountId);
    }
}
