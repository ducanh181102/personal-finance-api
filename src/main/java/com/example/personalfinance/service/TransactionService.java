package com.example.personalfinance.service;

import com.example.personalfinance.exception.ForbiddenException;
import com.example.personalfinance.exception.NotFoundException;
import com.example.personalfinance.model.*;
import com.example.personalfinance.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository, AccountService accountService, CategoryService categoryService, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    private Transaction getOwnedTransactionById(int id) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<Transaction> transaction = transactionRepository.findById(id);
        if (transaction.isEmpty()) throw new NotFoundException(id);
        if (transaction.get().getCategory().getUser().getId() != currentUser.getId())
            throw new ForbiddenException("Không có quyền truy cập.");
        return transaction.get();
    }

    public List<Transaction> getAllTransactions() {
        User currentUser = currentUserService.getCurrentUser();
        return transactionRepository.findByAccountUserId(currentUser.getId());
    }

    public Transaction getTransactionById(int id) {
        return getOwnedTransactionById(id);
    }

    @Transactional
    public Transaction updateTransaction(int id, BigDecimal amount, TransactionType type, String note) {
        Transaction transaction = getOwnedTransactionById(id);
        ensureManualTransaction(transaction);
        transaction.setAmount(amount != null ? amount : transaction.getAmount());
        transaction.setNote(note != null ? note : transaction.getNote());
        if (type != null && type != transaction.getCategory().getType()) {
            throw new IllegalArgumentException("Loại giao dịch phải trùng với loại danh mục.");
        }
        transaction.setType(type != null ? type : transaction.getType());
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createTransaction(int accountId, int categoryId, TransactionType type, BigDecimal amount, String note, LocalDateTime transactionDate) {
        return createTransaction(accountId, categoryId, type, amount, note, transactionDate, TransactionSource.MANUAL);
    }

    @Transactional
    public Transaction createTransaction(int accountId, int categoryId, TransactionType type, BigDecimal amount, String note, LocalDateTime transactionDate, TransactionSource source) {
        // Chỉ DEBT_PAYMENT_REVERSAL được phép dùng account archived,
        // vì đây là giao dịch đảo chiều để sửa lịch sử thanh toán nợ.
        Account account = source == TransactionSource.DEBT_PAYMENT_REVERSAL ? accountService.getAccountById(accountId) : accountService.getActiveAccountById(accountId);
        Category category = categoryService.getActiveCategoryById(categoryId);

        // Nghiệp vụ: kiểm tra account và category phải thuộc cùng 1 user
        if (account.getUser().getId() != category.getUser().getId())
            throw new IllegalArgumentException("Không cho giao dịch dùng category của user khác.");
        if (source == TransactionSource.MANUAL && category.isSystemCategory()) {
            throw new IllegalArgumentException("Không thể dùng danh mục hệ thống cho giao dịch nhập tay.");
        }
        if (category.getType() != type)
            throw new IllegalArgumentException("Loại danh mục phải trùng với loại giao dịch.");
        Transaction transaction = new Transaction(account, type, amount, transactionDate, note, category, source);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransactionById(int id) {
        Transaction transaction = getOwnedTransactionById(id);
        ensureManualTransaction(transaction);
        transactionRepository.delete(transaction);
    }

    private void ensureManualTransaction(Transaction transaction) {
        if (!transaction.isManual()) {
            throw new IllegalArgumentException("Không thể chỉnh sửa hoặc xóa transaction được tạo tự động.");
        }
    }

    @Transactional
    public List<Transaction> getTransactionsByAccountId(int accountId) {
        accountService.getAccountById(accountId);
        return transactionRepository.findByAccountId(accountId);
    }

    @Transactional
    public List<Transaction> findByAccountIdWithinPeriod(int accountId, LocalDate from, LocalDate to) {
        accountService.getAccountById(accountId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, start, end);
    }

    @Transactional
    public List<Transaction> findByCategoryIdWithinPeriod(int categoryId, LocalDate from, LocalDate to) {
        categoryService.findCategoryById(categoryId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByCategoryIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(categoryId, start, end);
    }

    public Page<Transaction> getTransactionsPage(int page, int size, Integer accountId, Integer categoryId, TransactionType type, TransactionSource source, BigDecimal minAmount, BigDecimal maxAmount, String keyword, String sort, LocalDate from, LocalDate to) {
        // Validate
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Cần gửi cả ngày bắt đầu và ngày kết thúc.");
        }

        if ((minAmount == null) != (maxAmount == null)) {
            throw new IllegalArgumentException("Cần gửi cả số tiền tối thiểu và tối đa.");
        }

        if (!sort.equals("DESC") && !sort.equals("ASC")) {
            throw new IllegalArgumentException("Yêu cầu sắp xếp không đúng cú pháp.");
        }

        if (page < 0) {
            throw new IllegalArgumentException("Vị trí trang không được nhỏ hơn 0.");
        } else if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Kích thước tối đa của 1 trang phải nằm trong khoảng từ 1 đến 100.");
        }


        Specification<Transaction> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn chỉ lấy transaction của user hiện tại
            predicates.add(criteriaBuilder.equal(root.get("account").get("user").get("id"), currentUserService.getCurrentUser().getId()));

            if (accountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("account").get("id"), accountId));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (source != null) {
                predicates.add(criteriaBuilder.equal(root.get("source"), source));
            }

            if (minAmount != null) {
                if (minAmount.compareTo(maxAmount) > 0) {
                    throw new IllegalArgumentException(("Số tiền tối thiểu không được lớn hơn số tiền tối đa."));
                }
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            if (from != null) {
                if (from.isAfter(to)) {
                    throw new IllegalArgumentException(("Ngày bắt đầu không được sau ngày kết thúc."));
                }

                LocalDateTime start = from.atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), start));
            }

            if (to != null) {
                LocalDateTime end = to.plusDays(1).atStartOfDay();
                predicates.add(criteriaBuilder.lessThan(root.get("transactionDate"), end));
            }

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), "%" + keyword.trim().toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };


        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("ASC".equals(sort) ? Sort.Order.asc("transactionDate") : Sort.Order.desc("transactionDate"), Sort.Order.desc("id")));

        return transactionRepository.findAll(specification, pageRequest);
    }

    public Page<Transaction> findPageByAccountIdWithinPeriod(int page, int size, int accountId, LocalDate from, LocalDate to) {
        accountService.getAccountById(accountId);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("id")));
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return transactionRepository.findByAccountIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThan(accountId, start, end, pageRequest);
    }

    @Transactional
    public List<Transaction> getTransactionsByCategoryId(int categoryId) {
        categoryService.findCategoryById(categoryId);
        return transactionRepository.findByCategoryId(categoryId);
    }
}
