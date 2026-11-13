package com.example.personalfinance.mapper;

import com.example.personalfinance.dto.account.AccountResponse;
import com.example.personalfinance.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    private final UserMapper userMapper;

    public AccountMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getOpeningBalance(),
                userMapper.toUserResponse(account.getUser())
        );
    }
}
