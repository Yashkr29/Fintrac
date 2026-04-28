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
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final InsightService insightService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public MonthlyReportDTO getMonthlyReport(int year, int month) {
        Long userId = authService.getCurrentUserId();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        String monthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, startDate, endDate);
        BigDecimal savings = totalIncome.subtract(totalExpense);

        BudgetDTO budget = budgetService.getBudgetByMonth(monthStr);
        BigDecimal budgetVsActual = budget.getAdjustedBudget().subtract(totalExpense);

        Double savingsRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = savings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        Map<String, BigDecimal> categoryWiseBreakdown = getCategoryWiseBreakdown(userId, startDate, endDate);
        Map<String, BigDecimal> merchantWiseSpending = getMerchantWiseSpending(userId, startDate, endDate);
        List<WeeklySummaryDTO> weeklySummaries = getWeeklySummaries(userId, startDate, endDate);
        List<InsightDTO> insights = insightService.generateInsights(userId);

        return MonthlyReportDTO.builder()
                .month(monthStr)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .savings(savings)
                .budgetVsActual(budgetVsActual)
                .savingsRate(savingsRate)
                .categoryWiseBreakdown(categoryWiseBreakdown)
                .merchantWiseSpending(merchantWiseSpending)
                .weeklySummaries(weeklySummaries)
                .insights(insights)
                .build();
    }

    private Map<String, BigDecimal> getCategoryWiseBreakdown(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> categoryData = transactionRepository.sumExpensesByCategory(userId, startDate, endDate);
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        for (Object[] row : categoryData) {
            String categoryName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (categoryName != null && amount != null) {
                breakdown.put(categoryName, amount);
            }
        }

        return breakdown;
    }

    private Map<String, BigDecimal> getMerchantWiseSpending(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> merchantData = transactionRepository.sumExpensesByMerchant(userId, startDate, endDate);
        Map<String, BigDecimal> merchantSpending = new LinkedHashMap<>();

        for (Object[] row : merchantData) {
            String merchantName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (merchantName != null && amount != null) {
                merchantSpending.put(merchantName, amount);
            }
        }

        return merchantSpending;
    }

    private List<WeeklySummaryDTO> getWeeklySummaries(Long userId, LocalDate startDate, LocalDate endDate) {
        List<com.fintrac.model.Transaction> transactions =
                transactionRepository.findTransactionsForPeriod(userId, startDate, endDate);

        Map<Integer, List<com.fintrac.model.Transaction>> transactionsByWeek = new TreeMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (com.fintrac.model.Transaction t : transactions) {
            int weekNum = t.getDate().get(weekFields.weekOfMonth());
            transactionsByWeek.computeIfAbsent(weekNum, k -> new ArrayList<>()).add(t);
        }

        List<WeeklySummaryDTO> summaries = new ArrayList<>();
        LocalDate tempDate = startDate;

        for (int weekNum : transactionsByWeek.keySet()) {
            List<com.fintrac.model.Transaction> weekTransactions = transactionsByWeek.get(weekNum);

            LocalDate weekStart = tempDate;
            LocalDate weekEnd = tempDate.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            BigDecimal weekIncome = weekTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(com.fintrac.model.Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal weekExpense = weekTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(com.fintrac.model.Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal weekSavings = weekIncome.subtract(weekExpense);

            summaries.add(WeeklySummaryDTO.builder()
                    .weekNumber(weekNum)
                    .weekRange(weekStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " +
                            weekEnd.format(DateTimeFormatter.ofPattern("MMM dd")))
                    .totalIncome(weekIncome)
                    .totalExpense(weekExpense)
                    .netSavings(weekSavings)
                    .build());

            tempDate = tempDate.plusWeeks(1);
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportDTO> getQuarterlyReport(int year, int quarter) {
        List<MonthlyReportDTO> reports = new ArrayList<>();
        int startMonth = (quarter - 1) * 3 + 1;

        for (int i = 0; i < 3; i++) {
            reports.add(getMonthlyReport(year, startMonth + i));
        }

        return reports;
    }
}
