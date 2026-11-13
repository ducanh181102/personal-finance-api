package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.Account;
import com.example.personalfinance.model.Transfer;
import com.example.personalfinance.model.User;
import com.example.personalfinance.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TransferService {
    private final TransferRepository transferRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;


    public TransferService(TransferRepository transferRepository, CurrentUserService currentUserService, AccountService accountService) {
        this.transferRepository = transferRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
    }

    private Transfer getOwnedTransferById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Transfer> transfer = transferRepository.findById(id);
        if (transfer.isEmpty()) throw new NotFoundException(id);
        if (transfer.get().getFromAccount().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return transfer.get();
    }

    public List<Transfer> getAllTransfer() {
        return transferRepository.findByFromAccountUserId(currentUserService.getCurrentUser().getId());
    }

    public Transfer getTransferById(int id) {
        return getOwnedTransferById(id);
    }

    @Transactional
    public Transfer createTransfer(Integer fromAccountId,
                                   Integer toAccountId,
                                   BigDecimal amount,
                                   LocalDateTime transferDate,
                                   String note) {

        if (Objects.equals(fromAccountId, toAccountId))
            throw new IllegalArgumentException("Tài khoản nhận và gửi không được trùng nhau.");

        Account fromAccount = accountService.getAccountById(fromAccountId);
        Account toAccount = accountService.getAccountById(toAccountId);

        Transfer transfer = new Transfer(
                fromAccount,
                toAccount,
                amount,
                transferDate,
                note
        );

        return transferRepository.save(transfer);
    }

    @Transactional
    public void deleteTransferById(int id) {
        Transfer transfer = getTransferById(id);
        transferRepository.delete(transfer);
    }

    public List<Transfer> getAllTransferByFromAccountId(int fromAccountId) {
        return transferRepository.findByFromAccountIdAndFromAccountUserId(fromAccountId, currentUserService.getCurrentUser().getId());
    }

    public List<Transfer> getAllTransferByToAccountId(int toAccountId) {
        return transferRepository.findByToAccountIdAndToAccountUserId(toAccountId, currentUserService.getCurrentUser().getId());
    }
}
