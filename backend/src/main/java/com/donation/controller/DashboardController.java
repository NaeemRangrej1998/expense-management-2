package com.donation.controller;

import com.donation.dto.response.DashboardSummaryResponse;
import com.donation.dto.response.PageResponse;
import com.donation.dto.response.TransactionResponse;
import com.donation.enums.TransactionStatus;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.DashboardService;
import com.donation.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('dashboard.view')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TransactionService transactionService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(dashboardService.getSummary(startDate, endDate, principal));
    }

    @GetMapping("/expense-categories")
    public ResponseEntity<List<DashboardSummaryResponse.ExpenseCategoryBreakdown>> getExpenseCategories(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(dashboardService.getExpenseCategories(startDate, endDate, principal));
    }

    @GetMapping("/daily-income")
    public ResponseEntity<PageResponse<TransactionResponse>> getDailyIncome(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) TransactionStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(transactionService.getDailyIncomeReport(reportDate, userId, status, page, size, principal));
    }
}
