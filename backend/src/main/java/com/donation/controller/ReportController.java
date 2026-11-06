package com.donation.controller;

import com.donation.enums.TransactionStatus;
import com.donation.enums.TransactionType;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final TransactionService transactionService;

    @GetMapping("/transactions/export")
    @PreAuthorize("hasAuthority('reports.view')")
    public ResponseEntity<byte[]> exportTransactions(
        @RequestParam(required = false) TransactionType type,
        @RequestParam(required = false) TransactionStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        String csv = transactionService.exportTransactionsToCsv(type, status, startDate, endDate, principal);
        byte[] data = csv.getBytes();
        
        String filename = "transactions_" + LocalDate.now() + ".csv";
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("text/csv"))
            .contentLength(data.length)
            .body(data);
    }
}
