package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.*;
import com.example.personalfinance.repository.CategoryRepository;
import com.example.personalfinance.repository.DebtPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DebtPaymentService {
    private final DebtPaymentRepository debtPaymentRepository;
    private final CurrentUserService currentUserService;
    private final DebtService debtService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public DebtPaymentService(DebtPaymentRepository debtPaymentRepository, CurrentUserService currentUserService, DebtService debtService, AccountService accountService, TransactionService transactionService, CategoryRepository categoryRepository, CategoryService categoryService) {
        this.debtPaymentRepository = debtPaymentRepository;
        this.currentUserService = currentUserService;
        this.debtService = debtService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
    }

    private DebtPayment getOwnedDebtPaymentById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<DebtPayment> debtPayment = debtPaymentRepository.findById(id);
        if (debtPayment.isEmpty()) throw new NotFoundException(id);
        if (debtPayment.get().getAccount().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return debtPayment.get();
    }

    public List<DebtPayment> getAllDebtPayments() {
        return debtPaymentRepository.findByAccountUserId(currentUserService.getCurrentUser().getId());
    }

    public DebtPayment getDebtPaymentById(int id) {
        return getOwnedDebtPaymentById(id);
    }

    @Transactional
    public DebtPayment createDebtPayment(Integer debtId,
                                         Integer accountId,
                                         BigDecimal amount,
                                         LocalDateTime paidAt,
                                         String note) {

        Debt debt = debtService.getDebtById(debtId);
        Account account = accountService.getActiveAccountById(accountId);
        categoryService.ensureDebtSystemCategories(account.getUser());

        // Tạo transaction
        TransactionType type = DebtType.BORROWED == debt.getType() ? TransactionType.EXPENSE : TransactionType.INCOME;
        String categoryName = DebtType.BORROWED == debt.getType() ? "TRẢ NỢ" : "THU HỒI NỢ";
        Category category = getSystemCategory(categoryName, type, account.getUser().getId());
        Transaction transaction = transactionService.createTransaction(
                account.getId(),
                category.getId(),
                type,
                amount,
                note,
                paidAt,
                TransactionSource.DEBT_PAYMENT
        );

        DebtPayment debtPayment = new DebtPayment(debt, account, amount, paidAt, note, transaction);
        // Cập nhật debt
        debt.applyPayment(amount);


        return debtPaymentRepository.save(debtPayment);
    }

    @Transactional
    public DebtPayment cancelDebtPaymentById(int id) {
        DebtPayment debtPayment = getDebtPaymentById(id);
        if (debtPayment.getStatus() == DebtPaymentStatus.CANCELLED) {
            throw new IllegalArgumentException("Thanh toán này đã bị hủy.");
        }

        Debt debt = debtPayment.getDebt();
        Account account = debtPayment.getAccount();
        categoryService.ensureDebtSystemCategories(account.getUser());
        debt.reversePayment(debtPayment.getAmount());

        // Tạo transaction đảo chiều
        TransactionType type = DebtType.BORROWED == debt.getType() ? TransactionType.INCOME : TransactionType.EXPENSE;
        String categoryName = DebtType.BORROWED == debt.getType() ? "HOÀN TRẢ NỢ" : "HOÀN THU HỒI NỢ";
        Category category = getSystemCategory(categoryName, type, account.getUser().getId());
        LocalDateTime cancelledAt = LocalDateTime.now();
        Transaction reversalTransaction = transactionService.createTransaction(
                account.getId(),
                category.getId(),
                type,
                debtPayment.getAmount(),
                "Hủy thanh toán nợ #" + debtPayment.getId(),
                cancelledAt,
                TransactionSource.DEBT_PAYMENT_REVERSAL
        );
        debtPayment.cancelDebtPayment(reversalTransaction, cancelledAt);

        return debtPayment;
    }

    private Category getSystemCategory(String name, TransactionType type, int userId) {
        return categoryRepository.findByNameAndTypeAndUserIdAndIsSystemCategoryTrue(name, type, userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy danh mục hệ thống: " + name));
    }

}
