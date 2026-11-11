package com.example.personalfinance.repository;

import com.example.personalfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(String email);

    Optional<User> findUserByEmail(String email);
}
