package com.example.personalfinance.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private boolean active = true;
    @Column(unique = true, length = 64)
    private String passwordResetHash;
    private Instant passwordResetExpiresAt;
    @Column(length = 36)
    private String sessionVersion;

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
        clearPasswordReset();
    }

    public void activate() {
        this.active = true;
    }

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
        clearPasswordReset();
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.sessionVersion = UUID.randomUUID().toString();
        clearPasswordReset();
    }

    public void issuePasswordReset(String hash, Instant expiresAt) {
        passwordResetHash = hash;
        passwordResetExpiresAt = expiresAt;
    }

    private void clearPasswordReset() {
        passwordResetHash = null;
        passwordResetExpiresAt = null;
    }

    @JsonIgnore
    public Instant getPasswordResetExpiresAt() {
        return passwordResetExpiresAt;
    }

    @JsonIgnore
    public String getSessionVersion() {
        return sessionVersion;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }
}
