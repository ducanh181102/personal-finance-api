package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtType;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.DebtPaymentRepository;
import com.example.personalfinance.repository.DebtRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DebtService {
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;
    private final DebtPaymentRepository debtPaymentRepository;

    public DebtService(DebtRepository debtRepository, CurrentUserService currentUserService, DebtPaymentRepository debtPaymentRepository) {
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
        this.debtPaymentRepository = debtPaymentRepository;
    }

    private Debt getOwnedDebtById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Debt> debt = debtRepository.findById(id);
        if (debt.isEmpty()) throw new NotFoundException(id);
        if (debt.get().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return debt.get();
    }

    public List<Debt> getAllDebts() {
        return debtRepository.findByUserId(currentUserService.getCurrentUser().getId());
    }

    public Debt getDebtById(int id) {
        return getOwnedDebtById(id);
    }

    public Debt createDebt(String personName,
                           DebtType type,
                           BigDecimal originalAmount,
                           LocalDate dueDate,
                           String note) {


        Debt debt = new Debt(currentUserService.getCurrentUser(), personName, type, originalAmount, dueDate, note);
        return debtRepository.save(debt);
    }

    public Debt updateDebtById(int id, String personName, LocalDate dueDate, String note) {
        Debt debt = getDebtById(id);
        if (personName != null) {
            debt.setPersonName(personName);
        }

        if (dueDate != null) {
            debt.setDueDate(dueDate);
        }

        if (note != null) {
            debt.setNote(note);
        }

        return debtRepository.save(debt);
    }

    public void deleteDebtById(int id) {
        Debt debt = getDebtById(id);
        if (debtPaymentRepository.existsByDebtId(id))
            throw new IllegalArgumentException("Không thể xoá nợ đã có thanh toán.");
        debtRepository.delete(debt);
    }
}
