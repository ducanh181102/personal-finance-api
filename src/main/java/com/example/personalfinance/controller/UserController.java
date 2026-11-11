package com.example.personalfinance.controller;

import com.example.personalfinance.dto.*;
import com.example.personalfinance.mapper.AccountMapper;
import com.example.personalfinance.mapper.CategoryMapper;
import com.example.personalfinance.mapper.UserMapper;
import com.example.personalfinance.model.User;
import com.example.personalfinance.service.AccountService;
import com.example.personalfinance.service.CategoryService;
import com.example.personalfinance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AccountService accountService;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public UserController(UserService userService, AccountService accountService, UserMapper userMapper, AccountMapper accountMapper, CategoryService categoryService, CategoryMapper categoryMapper) {
        this.userService = userService;
        this.accountService = accountService;
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUser() {
        User user = userService.getUser();
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }

    // Đình chỉ
//    @GetMapping("/{id}/accounts")
//    public List<AccountResponse> getAllAccountById(@PathVariable("id") int id) {
//        return accountService.getAccountsByUserId(id).stream().map(
//                accountMapper::toAccountResponse
//        ).toList();
//    }

    @GetMapping("/accounts/me")
    public List<AccountResponse> getAllAccount() {
        return accountService.getAllAccounts().stream().map(
                accountMapper::toAccountResponse
        ).toList();
    }

    // Đình chỉ
//    @GetMapping("/{id}/categories")
//    public List<CategoryResponse> getAllCategoryById(@PathVariable("id") int id) {
//        return categoryService.getCategoriesByUserId(id).stream().map(categoryMapper::toCategoryResponse).toList();
//    }

    @GetMapping("/categories/me")
    public List<CategoryResponse> getAllCategory() {
        return categoryService.getCategories().stream().map(categoryMapper::toCategoryResponse).toList();
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toUserResponse(user));
    }

    // Đình chỉ
//    @PatchMapping("/{id}")
//    public ResponseEntity<UserResponse> updateUserById(@PathVariable("id") int id, @Valid @RequestBody UpdateUserRequest request) {
//        User user = userService.updateUserById(id, request.getName(), request.getEmail(), request.getPassword());
//        return ResponseEntity.ok(userMapper.toUserResponse(user));
//    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateUser(@Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }

    // Đình chỉ
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteUserById(@PathVariable("id") int id) {
//        userService.deleteUserById(id);
//        return ResponseEntity.noContent().build();
//    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser() {
        userService.deleteUser();
        return ResponseEntity.noContent().build();
    }
}
