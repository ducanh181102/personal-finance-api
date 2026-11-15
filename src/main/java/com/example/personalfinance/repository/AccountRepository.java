package com.example.personalfinance.repository;

import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    List<Account> findByUserId(Integer id);
    List<Account> findByUserIdAndStatus(Integer id, AccountStatus accountStatus);
}
