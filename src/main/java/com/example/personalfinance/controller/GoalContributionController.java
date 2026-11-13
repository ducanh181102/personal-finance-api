package com.example.personalfinance.controller;

import com.example.personalfinance.dto.goalcontribution.CreateGoalContributionRequest;
import com.example.personalfinance.dto.goalcontribution.GoalContributionResponse;
import com.example.personalfinance.mapper.GoalContributionMapper;
import com.example.personalfinance.model.GoalContribution;
import com.example.personalfinance.service.GoalContributionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goal-contributions")
public class GoalContributionController {
    private final GoalContributionService goalContributionService;
    private final GoalContributionMapper goalContributionMapper;

    public GoalContributionController(GoalContributionService goalContributionService, GoalContributionMapper goalContributionMapper) {
        this.goalContributionService = goalContributionService;
        this.goalContributionMapper = goalContributionMapper;
    }

    @GetMapping
    public List<GoalContributionResponse> getAllGoalContributions() {
        return goalContributionService.getAllGoalContributions().stream().map(goalContributionMapper::toGoalContributionResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalContributionResponse> getGoalContributionById(@PathVariable("id") int id) {
        GoalContributionResponse goalContributionResponse = goalContributionMapper.toGoalContributionResponse(goalContributionService.getGoalContributionById(id));
        return ResponseEntity.ok(goalContributionResponse);
    }

    @PostMapping
    public ResponseEntity<GoalContributionResponse> createGoalContribution(@Valid @RequestBody CreateGoalContributionRequest request) {
        GoalContribution goalContribution = goalContributionService.createGoalContribution(request.getSavingsGoalId(),
                request.getAccountId(),
                request.getAmount(),
                request.getContributedAt(),
                request.getNote());
        GoalContributionResponse goalContributionResponse = goalContributionMapper.toGoalContributionResponse(goalContribution);
        return ResponseEntity.status(HttpStatus.CREATED).body(goalContributionResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoalContributionById(@PathVariable("id") int id) {
        goalContributionService.deleteGoalContributionById(id);
        return ResponseEntity.noContent().build();
    }
}
