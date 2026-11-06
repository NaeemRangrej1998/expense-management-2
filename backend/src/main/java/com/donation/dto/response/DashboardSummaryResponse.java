package com.donation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal remainingBalance;
    private String dateRange;
    private String startDate;
    private String endDate;

    @Data
    @Builder
    public static class ExpenseCategoryBreakdown {
        private String category;
        private BigDecimal amount;
        private Double percentage;
    }
}
