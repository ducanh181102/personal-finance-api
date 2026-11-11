package com.example.personalfinance.service;

import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    private boolean isExistEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Đình chỉ
//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }

    public User getUser() {
        return currentUserService.getCurrentUser();
    }

    // Đình chỉ
//    public User getUserById(int id) {
//        Optional<User> user = userRepository.findById(id);
//        if (user.isEmpty()) throw new NotFoundException(id);
//        return user.get();
//    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    @Transactional
    public User createUser(String name, String email, String password) {
        if (isExistEmail(email)) throw new IllegalArgumentException("Email đã tồn tại.");
        String passwordHash = passwordEncoder.encode(password);
        return userRepository.save(new User(name, email, passwordHash));
    }

    @Transactional
    public User updateUser(String name, String email, String password) {
        User user = currentUserService.getCurrentUser();
        if (email != null && !user.getEmail().equals(email)) {
            if (isExistEmail(email)) throw new IllegalArgumentException("Email đã tồn tại.");
            user.setEmail(email);
        }
        user.setName(name != null ? name : user.getName());
        if (password != null) {
            String passwordHash = passwordEncoder.encode(password);
            user.setPasswordHash(passwordHash);
        }
        return userRepository.save(user);
    }

    // Đình chỉ
//    @Transactional
//    public User updateUserById(int id, String name, String email, String password) {
//        User user = getUserById(id);
//        if (email != null && !user.getEmail().equals(email)) {
//            if (isExistEmail(email)) throw new IllegalArgumentException("Email đã tồn tại.");
//            user.setEmail(email);
//        }
//        user.setName(name != null ? name : user.getName());
//        if (password != null) {
//            String passwordHash = passwordEncoder.encode(password);
//            user.setPasswordHash(passwordHash);
//        }
//        return userRepository.save(user);
//    }


    // Đình chỉ
//    @Transactional
//    public void deleteUserById(int id) {
//        User user = getUserById(id);
//        userRepository.delete(user);
//    }

    @Transactional
    public void deleteUser() {
        User user = currentUserService.getCurrentUser();
        user.deactivate();
        userRepository.save(user);
    }
}
