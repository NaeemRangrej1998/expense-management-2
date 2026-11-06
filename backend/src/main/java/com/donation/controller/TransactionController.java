package com.donation.controller;

import com.donation.dto.request.ApprovalRequest;
import com.donation.dto.request.CreateTransactionRequest;
import com.donation.dto.response.PageResponse;
import com.donation.dto.response.TransactionResponse;
import com.donation.enums.TransactionStatus;
import com.donation.enums.TransactionType;
import com.donation.exception.ApiException;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.AttachmentService;
import com.donation.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionResponse> createTransactionWithoutAttachment() {
        throw ApiException.badRequest("A bill or receipt attachment is required to create a transaction");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionResponse> createTransaction(
        @Valid @RequestPart("transaction") CreateTransactionRequest request,
        @RequestPart("file") MultipartFile file,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {
        return ResponseEntity.status(201).body(transactionService.createTransaction(request, file, principal));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactions(
        @RequestParam TransactionType type,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) TransactionStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Long createdById,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(
            type, search, status, startDate, endDate, createdById, page, size, principal
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.getById(id, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
        @PathVariable Long id,
        @Valid @RequestBody CreateTransactionRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, request, principal));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<TransactionResponse> approve(
        @PathVariable Long id,
        @RequestBody(required = false) ApprovalRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.approveTransaction(
            id, request != null ? request : new ApprovalRequest(), principal
        ));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<TransactionResponse> reject(
        @PathVariable Long id,
        @RequestBody(required = false) ApprovalRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.rejectTransaction(
            id, request != null ? request : new ApprovalRequest(), principal
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<TransactionResponse>> getHistory(
        @RequestParam(required = false) TransactionType type,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.getHistory(
            type, search, startDate, endDate, page, size, principal
        ));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<Map<String, Object>>> getAttachments(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(attachmentService.getTransactionAttachments(id, principal));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {
        var attachment = attachmentService.uploadAttachment(id, file, principal);
        return ResponseEntity.status(201).body(Map.of(
            "id", attachment.getId(),
            "originalFileName", attachment.getOriginalFileName(),
            "storageKey", attachment.getStorageKey(),
            "fileSize", attachment.getFileSize()
        ));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
        @PathVariable Long attachmentId,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Resource resource = attachmentService.loadAttachment(attachmentId, principal);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
}
