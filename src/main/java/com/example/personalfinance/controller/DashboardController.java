package com.example.personalfinance.controller;

import com.example.personalfinance.dto.dashboard.MonthlyDashboardResponse;
import com.example.personalfinance.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/monthly")
    public MonthlyDashboardResponse getMonthlyDashboard(@RequestParam("month") LocalDate month) {
        return dashboardService.getMonthlyDashboard(month);
    }
}
