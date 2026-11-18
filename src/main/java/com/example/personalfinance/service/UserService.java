package com.example.personalfinance.service;

import com.example.personalfinance.exception.InvalidCredentialsException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentUserService currentUserService, CategoryService categoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.categoryService = categoryService;
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
        User user = userRepository.save(new User(name, email, passwordHash));
        // Tạo category hệ thống
        categoryService.ensureDebtSystemCategories(user);
        return user;
    }

    @Transactional
    public User updateUser(String name, String email, String password, String currentPassword) {
        User user = lockCurrentUser();
        boolean changingEmail = email != null && !user.getEmail().equals(email);
        // Yêu cầu xác thực lại cho mọi đường dẫn có thể đổi mật khẩu hoặc địa chỉ nhận thư khôi phục.
        if (changingEmail || password != null) verifyCurrentPassword(user, currentPassword);
        if (changingEmail) {
            if (email.isBlank()) throw new IllegalArgumentException("Email không được trống.");
            if (isExistEmail(email)) throw new IllegalArgumentException("Email đã tồn tại.");
            user.setEmail(email);
        }
        if (name != null) {
            if (name.isBlank()) throw new IllegalArgumentException("Tên tài khoản không được trống.");
            user.setName(name.strip());
        }
        if (password != null) setNewPassword(user, password);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User user = lockCurrentUser();
        verifyCurrentPassword(user, currentPassword);
        setNewPassword(user, newPassword);
    }

    private void verifyCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Nhập mật khẩu hiện tại.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
    }

    private void setNewPassword(User user, String newPassword) {
        if (newPassword.isBlank()) throw new IllegalArgumentException("Nhập mật khẩu mới.");
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu tối đa 72 byte UTF-8.");
        }
        // User.setPasswordHash đổi sessionVersion và hủy liên kết khôi phục đã cấp trước đó.
        user.setPasswordHash(passwordEncoder.encode(newPassword));
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
        User user = lockCurrentUser();
        user.deactivate();
        userRepository.save(user);
    }

    private User lockCurrentUser() {
        User principal = currentUserService.getCurrentUser();
        int id = principal.getId();
        User user = userRepository.lockById(id).orElseThrow(() -> new NotFoundException(id));
        if (!user.isActive() || !Objects.equals(user.getSessionVersion(), principal.getSessionVersion())) {
            throw new InvalidCredentialsException("Không thể xác thực người dùng.");
        }
        return user;
    }
}
