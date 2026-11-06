package com.donation.service;

import com.donation.dto.response.DashboardSummaryResponse;
import com.donation.entity.Area;
import com.donation.entity.User;
import com.donation.repository.TransactionRepository;
import com.donation.repository.UserRepository;
import com.donation.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(
        LocalDate startDate,
        LocalDate endDate,
        CustomUserPrincipal principal
    ) {
        List<Long> areaIds = getAuthorizedAreaIds(principal);

        BigDecimal totalIncome;
        BigDecimal totalExpenses;

        if (startDate != null && endDate != null) {
            totalIncome = transactionRepository.sumApprovedIncomeByDateRange(startDate, endDate, areaIds);
            totalExpenses = transactionRepository.sumApprovedExpensesByDateRange(startDate, endDate, areaIds);
        } else {
            totalIncome = transactionRepository.sumApprovedIncome(areaIds);
            totalExpenses = transactionRepository.sumApprovedExpenses(areaIds);
        }

        BigDecimal remaining = totalIncome.subtract(totalExpenses);

        return DashboardSummaryResponse.builder()
            .totalIncome(totalIncome)
            .totalExpenses(totalExpenses)
            .remainingBalance(remaining)
            .startDate(startDate != null ? startDate.toString() : null)
            .endDate(endDate != null ? endDate.toString() : null)
            .build();
    }

    @Transactional(readOnly = true)
    public List<DashboardSummaryResponse.ExpenseCategoryBreakdown> getExpenseCategories(
        LocalDate startDate,
        LocalDate endDate,
        CustomUserPrincipal principal
    ) {
        List<Long> areaIds = getAuthorizedAreaIds(principal);
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<Object[]> results = transactionRepository.sumExpensesByCategory(start, end, areaIds);

        BigDecimal total = results.stream()
            .map(r -> (BigDecimal) r[1])
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardSummaryResponse.ExpenseCategoryBreakdown> breakdowns = new ArrayList<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            double percentage = total.compareTo(BigDecimal.ZERO) > 0
                ? amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

            breakdowns.add(DashboardSummaryResponse.ExpenseCategoryBreakdown.builder()
                .category(category)
                .amount(amount)
                .percentage(percentage)
                .build());
        }
        return breakdowns;
    }

    private List<Long> getAuthorizedAreaIds(CustomUserPrincipal principal) {
        if (principal.isSuperAdmin()) return null;
        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        List<Long> ids = user.getAreas().stream().map(Area::getId).collect(Collectors.toList());
        return ids.isEmpty() ? List.of(-1L) : ids;
    }
}
