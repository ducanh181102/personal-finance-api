package com.example.personalfinance.controller;

import com.example.personalfinance.dto.debt.CreateDebtRequest;
import com.example.personalfinance.dto.debt.DebtResponse;
import com.example.personalfinance.dto.debt.UpdateDebtRequest;
import com.example.personalfinance.mapper.DebtMapper;
import com.example.personalfinance.model.Debt;
import com.example.personalfinance.service.DebtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
public class DebtController {
    private final DebtService debtService;
    private final DebtMapper debtMapper;

    public DebtController(DebtService debtService, DebtMapper debtMapper) {
        this.debtService = debtService;
        this.debtMapper = debtMapper;
    }

    @GetMapping
    public List<DebtResponse> getAllDebts() {
        List<Debt> debts = debtService.getAllDebts();
        return debts.stream().map(debtMapper::toDebtResponse).toList();
    }

    @GetMapping("{id}")
    public ResponseEntity<DebtResponse> getDebtById(@PathVariable("id") int id) {
        Debt debt = debtService.getDebtById(id);
        DebtResponse debtResponse = debtMapper.toDebtResponse(debt);
        return ResponseEntity.ok(debtResponse);
    }

    @PostMapping
    public ResponseEntity<DebtResponse> createDebt(@Valid @RequestBody CreateDebtRequest request) {
        Debt debt = debtService.createDebt(request.getPersonName(),
                request.getType(),
                request.getOriginalAmount(),
                request.getDueDate(),
                request.getNote());
        DebtResponse debtResponse = debtMapper.toDebtResponse(debt);
        return ResponseEntity.status(HttpStatus.CREATED).body(debtResponse);
    }

    @PatchMapping("{id}")
    public ResponseEntity<DebtResponse> updateDebtById(@PathVariable("id") int id, @Valid @RequestBody UpdateDebtRequest request) {
        Debt debt = debtService.updateDebtById(id, request.getPersonName(),
                request.getDueDate(),
                request.getNote());
        DebtResponse debtResponse = debtMapper.toDebtResponse(debt);
        return ResponseEntity.ok(debtResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDebtById(@PathVariable("id") int id) {
        debtService.deleteDebtById(id);
        return ResponseEntity.noContent().build();
    }
}
