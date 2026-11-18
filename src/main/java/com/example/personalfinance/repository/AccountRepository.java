package com.example.personalfinance.repository;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    List<Account> findByUserId(Integer id);
    List<Account> findByUserIdAndStatus(Integer id, AccountStatus accountStatus);

    @Query("select coalesce(sum(a.openingBalance), 0) from Account a where a.user.id = :userId")
    BigDecimal sumOpeningBalancesByUserId(int userId);
}
