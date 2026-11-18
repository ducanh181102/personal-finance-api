package com.example.personalfinance.controller;

import com.example.personalfinance.dto.report.CashFlowForecastResponse;
import com.example.personalfinance.dto.report.MonthlyCashFlowReport;
import com.example.personalfinance.service.CashFlowForecastService;
import com.example.personalfinance.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private final CashFlowForecastService cashFlowForecastService;

    public ReportController(ReportService reportService, CashFlowForecastService cashFlowForecastService) {
        this.reportService = reportService;
        this.cashFlowForecastService = cashFlowForecastService;
    }

    @GetMapping("/monthly")
    public MonthlyCashFlowReport getMonthlyReport(@RequestParam("month") LocalDate month) {
        return reportService.getMonthlyReport(month);
    }

    @GetMapping("/cash-flow-forecast")
    public CashFlowForecastResponse getCashFlowForecast(@RequestParam(name = "days", defaultValue = "30") int days) {
        return cashFlowForecastService.getForecast(days);
    }
}
