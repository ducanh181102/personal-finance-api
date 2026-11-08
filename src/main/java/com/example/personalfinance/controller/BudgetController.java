package com.example.personalfinance.controller;

import com.example.personalfinance.dto.BudgetSummaryResponse;
import com.example.personalfinance.dto.CreateBudgetRequest;
import com.example.personalfinance.dto.UpdateBudgetRequest;
import com.example.personalfinance.model.Budget;
import com.example.personalfinance.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<Budget> getAllBudgets() {
        return budgetService.getAllBudgets();
    }

    @GetMapping("/summary")
    public List<BudgetSummaryResponse> getBudgetSummaryByMonth(@RequestParam LocalDate month) {
        return budgetService.getBudgetSummaryByMonth(month);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudgetById(@PathVariable("id") int id) {
        Budget budget = budgetService.getBudgetById(id);
        return ResponseEntity.ok(budget);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummaryById(@PathVariable("id") int id) {
        BudgetSummaryResponse budgetSummaryResponse = budgetService.getBudgetSummary(id);
        return ResponseEntity.ok(budgetSummaryResponse);
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        Budget budget = budgetService.createBudget(request.getCategoryId(), request.getMonth(), request.getLimitAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(budget);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Budget> updateBudgetById(@PathVariable("id") int id, @Valid @RequestBody UpdateBudgetRequest request) {
        Budget budget = budgetService.updateBudgetById(id, request.getLimitAmount());
        return ResponseEntity.ok(budget);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudgetById(@PathVariable("id") int id) {
        budgetService.deleteBudgetById(id);
        return ResponseEntity.noContent().build();
    }
}
