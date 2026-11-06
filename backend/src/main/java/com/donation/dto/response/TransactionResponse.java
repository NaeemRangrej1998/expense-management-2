package com.donation.dto.response;

import com.donation.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionNumber;
    private String type;
    private BigDecimal amount;
    private CategoryInfo incomeCategory;
    private CategoryInfo expenseCategory;
    private AreaInfo area;
    private UserInfo createdBy;
    private LocalDate transactionDate;
    private String paymentMethod;
    private String referenceNumber;
    private String description;
    private String status;
    private UserInfo approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class AreaInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
    }

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
            .id(t.getId())
            .transactionNumber(t.getTransactionNumber())
            .type(t.getType().name())
            .amount(t.getAmount())
            .incomeCategory(t.getIncomeCategory() != null
                ? CategoryInfo.builder()
                    .id(t.getIncomeCategory().getId())
                    .name(t.getIncomeCategory().getName())
                    .build()
                : null)
            .expenseCategory(t.getExpenseCategory() != null
                ? CategoryInfo.builder()
                    .id(t.getExpenseCategory().getId())
                    .name(t.getExpenseCategory().getName())
                    .build()
                : null)
            .area(t.getArea() != null
                ? AreaInfo.builder().id(t.getArea().getId()).name(t.getArea().getName()).build()
                : null)
            .createdBy(UserInfo.builder()
                .id(t.getCreatedBy().getId())
                .name(t.getCreatedBy().getName())
                .email(t.getCreatedBy().getEmail())
                .build())
            .transactionDate(t.getTransactionDate())
            .paymentMethod(t.getPaymentMethod().name())
            .referenceNumber(t.getReferenceNumber())
            .description(t.getDescription())
            .status(t.getStatus().name())
            .approvedBy(t.getApprovedBy() != null
                ? UserInfo.builder()
                    .id(t.getApprovedBy().getId())
                    .name(t.getApprovedBy().getName())
                    .email(t.getApprovedBy().getEmail())
                    .build()
                : null)
            .approvedAt(t.getApprovedAt())
            .rejectionReason(t.getRejectionReason())
            .createdAt(t.getCreatedAt())
            .updatedAt(t.getUpdatedAt())
            .build();
    }
}
