package com.fintrac.service;

import com.fintrac.dto.*;
import com.fintrac.model.TransactionType;
import com.fintrac.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final AlertService alertService;
    private final InsightService insightService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public DashboardDTO getDashboardData() {
        Long userId = authService.getCurrentUserId();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        String monthStr = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, startDate, endDate);

        BudgetDTO budget = budgetService.getBudgetByMonth(monthStr);
        BigDecimal remainingBudget = budget.getRemainingBudget() != null ? budget.getRemainingBudget() : BigDecimal.ZERO;
        BigDecimal savings = totalIncome.subtract(totalExpense);
        Double budgetUsagePercentage = budget.getUsagePercentage() != null ? budget.getUsagePercentage() : 0.0;

        List<AlertDTO> alerts = alertService.getUnreadAlerts().stream().limit(5).collect(Collectors.toList());
        List<InsightDTO> insights = insightService.generateInsights(userId).stream().limit(5).collect(Collectors.toList());

        Map<String, BigDecimal> categoryBreakdown = getCategoryBreakdown(userId, startDate, endDate);
        Map<String, BigDecimal> weeklyTrend = getWeeklyTrend(userId, startDate, endDate);

        String budgetStatus = budget.getStatus() != null ? budget.getStatus() : "NO_BUDGET";

        return DashboardDTO.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .remainingBudget(remainingBudget)
                .savings(savings)
                .budgetUsagePercentage(budgetUsagePercentage)
                .budgetStatus(budgetStatus)
                .recentAlerts(alerts)
                .insights(insights)
                .categoryBreakdown(categoryBreakdown)
                .weeklyTrend(weeklyTrend)
                .build();
    }

    private Map<String, BigDecimal> getCategoryBreakdown(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> categoryData = transactionRepository.sumExpensesByCategory(userId, startDate, endDate);
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        for (Object[] row : categoryData) {
            String categoryName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (categoryName != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                breakdown.put(categoryName, amount);
            }
        }

        return breakdown;
    }

    private Map<String, BigDecimal> getWeeklyTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        List<com.fintrac.model.Transaction> transactions =
                transactionRepository.findTransactionsForPeriod(userId, startDate, endDate);

        Map<Integer, BigDecimal> weeklyExpenses = new TreeMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (com.fintrac.model.Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                int weekNum = t.getDate().get(weekFields.weekOfMonth());
                weeklyExpenses.merge(weekNum, t.getAmount(), BigDecimal::add);
            }
        }

        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        for (Map.Entry<Integer, BigDecimal> entry : weeklyExpenses.entrySet()) {
            trend.put("Week " + entry.getKey(), entry.getValue());
        }

        return trend;
    }
}
