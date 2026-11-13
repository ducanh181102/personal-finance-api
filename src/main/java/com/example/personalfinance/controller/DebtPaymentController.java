package com.example.personalfinance.controller;

import com.example.personalfinance.dto.debtpayment.CreateDebtPaymentRequest;
import com.example.personalfinance.dto.debtpayment.DebtPaymentResponse;
import com.example.personalfinance.mapper.DebtPaymentMapper;
import com.example.personalfinance.model.DebtPayment;
import com.example.personalfinance.service.DebtPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debt-payments")
public class DebtPaymentController {
    private final DebtPaymentService debtPaymentService;
    private final DebtPaymentMapper debtPaymentMapper;

    public DebtPaymentController(DebtPaymentService debtPaymentService, DebtPaymentMapper debtPaymentMapper) {
        this.debtPaymentService = debtPaymentService;
        this.debtPaymentMapper = debtPaymentMapper;
    }

    @GetMapping
    public List<DebtPaymentResponse> getAllDebtPayments() {
        return debtPaymentService.getAllDebtPayments().stream().map(debtPaymentMapper::toDebtPaymentResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DebtPaymentResponse> getDebtPaymentById(@PathVariable("id") int id) {
        DebtPayment debtPayment = debtPaymentService.getDebtPaymentById(id);
        DebtPaymentResponse debtPaymentResponse = debtPaymentMapper.toDebtPaymentResponse(debtPayment);

        return ResponseEntity.ok(debtPaymentResponse);
    }

    @PostMapping
    public ResponseEntity<DebtPaymentResponse> createDebtPayment(@Valid @RequestBody CreateDebtPaymentRequest request) {
        DebtPayment debtPayment = debtPaymentService.createDebtPayment(request.getDebtId(),
                request.getAccountId(),
                request.getAmount(),
                request.getPaidAt(),
                request.getNote()
        );
        DebtPaymentResponse debtPaymentResponse = debtPaymentMapper.toDebtPaymentResponse(debtPayment);

        return ResponseEntity.status(HttpStatus.CREATED).body(debtPaymentResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DebtPaymentResponse> deleteDebtPaymentById(@PathVariable("id") int id) {
        debtPaymentService.deleteDebtPaymentById(id);
        return ResponseEntity.noContent().build();
    }
}
