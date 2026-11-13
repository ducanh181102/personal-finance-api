package com.example.personalfinance.controller;

import com.example.personalfinance.dto.savingsgoal.CreateSavingsGoalRequest;
import com.example.personalfinance.dto.savingsgoal.SavingsGoalResponse;
import com.example.personalfinance.dto.savingsgoal.UpdateSavingsGoalRequest;
import com.example.personalfinance.mapper.SavingsGoalMapper;
import com.example.personalfinance.model.SavingsGoal;
import com.example.personalfinance.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {
    private final SavingsGoalService savingsGoalService;
    private final SavingsGoalMapper savingsGoalMapper;

    public SavingsGoalController(SavingsGoalService savingsGoalService, SavingsGoalMapper savingsGoalMapper) {
        this.savingsGoalService = savingsGoalService;
        this.savingsGoalMapper = savingsGoalMapper;
    }

    @GetMapping
    public List<SavingsGoalResponse> getAllSavingsGoals() {
        return savingsGoalService.getAllSavingsGoal().stream().map(savingsGoalMapper::toSavingsGoalResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> getSavingsGoalById(@PathVariable("id") int id) {
        SavingsGoal savingsGoal = savingsGoalService.getSavingsGoal(id);
        SavingsGoalResponse savingsGoalResponse = savingsGoalMapper.toSavingsGoalResponse(savingsGoal);
        return ResponseEntity.ok(savingsGoalResponse);
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> createSavingsGoal(@Valid @RequestBody CreateSavingsGoalRequest request) {
        SavingsGoal savingsGoal = savingsGoalService.createSavingsGoal(
                request.getName(),
                request.getTargetAmount(),
                request.getDeadline()
        );
        SavingsGoalResponse savingsGoalResponse = savingsGoalMapper.toSavingsGoalResponse(savingsGoal);
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsGoalResponse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> updateSavingsGoalById(@PathVariable("id") int id, @Valid @RequestBody UpdateSavingsGoalRequest request) {
        SavingsGoal savingsGoal = savingsGoalService.updateSavingsGoalById(id,
                request.getName(),
                request.getTargetAmount(),
                request.getDeadline());
        SavingsGoalResponse savingsGoalResponse = savingsGoalMapper.toSavingsGoalResponse(savingsGoal);
        return ResponseEntity.ok(savingsGoalResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavingsGoalById(@PathVariable("id") int id) {
        savingsGoalService.deleteSavingsGoalById(id);
        return ResponseEntity.noContent().build();
    }
}
