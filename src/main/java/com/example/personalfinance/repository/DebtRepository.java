package com.example.personalfinance.repository;

import com.example.personalfinance.model.Debt;
import com.example.personalfinance.model.DebtStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<Debt, Integer> {
    List<Debt> findByUserId(int userId);

    List<Debt> findByUserIdAndStatus(int userId, DebtStatus status);

    @Query("""
            select debt.id from Debt debt
            where debt.installmentEmailReminderEnabled = true and debt.user.active = true
              and debt.status = :status and debt.dueDate >= :today and debt.firstInstallmentDate <= :cutoff
            """)
    List<Integer> findInstallmentReminderCandidates(DebtStatus status, LocalDate today, LocalDate cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select debt from Debt debt where debt.id = :id")
    Optional<Debt> findForUpdateById(int id);

    List<Debt> findByUserIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(
            int userId,
            DebtStatus status,
            LocalDate dueDate
    );
}
