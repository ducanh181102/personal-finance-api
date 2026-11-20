package com.example.personalfinance.repository;

import com.example.personalfinance.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(String email);

    Optional<User> findUserByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") int id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateByPasswordResetHash(String passwordResetHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateByEmailVerifyHash(String emailVerifyHash);
}
