package com.fintrac.service;

import com.fintrac.dto.InsightDTO;
import com.fintrac.model.Transaction;
import com.fintrac.model.TransactionType;
import com.fintrac.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<InsightDTO> generateInsights(Long userId) {
        List<InsightDTO> insights = new ArrayList<>();

        insights.addAll(analyzeCategorySpending(userId));
        insights.addAll(analyzeMerchantSpending(userId));
        insights.addAll(analyzeWeeklyTrend(userId));
        insights.addAll(compareWithPreviousMonth(userId));
        insights.addAll(detectSpendingSpikes(userId));

        return insights;
    }

    private List<InsightDTO> analyzeCategorySpending(Long userId) {
        List<InsightDTO> insights = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        List<Object[]> categoryExpenses = transactionRepository.sumExpensesByCategory(userId, startDate, endDate);

        if (categoryExpenses.isEmpty()) {
            return insights;
        }

        BigDecimal totalExpense = categoryExpenses.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Object[] row : categoryExpenses) {
            String categoryName = (String) row[0];
            BigDecimal categoryAmount = (BigDecimal) row[1];

            if (categoryAmount == null || categoryAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            double percentage = categoryAmount.divide(totalExpense, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            if (percentage > 30) {
                insights.add(InsightDTO.builder()
                        .type("CATEGORY_ALERT")
                        .title("High Spending on " + categoryName)
                        .message(String.format("You've spent %.1f%% of your expenses on %s this month. Total: ₹%.2f",
                                percentage, categoryName, categoryAmount))
                        .severity("warning")
                        .build());
            }
        }

        return insights;
    }

    private List<InsightDTO> analyzeMerchantSpending(Long userId) {
        List<InsightDTO> insights = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        List<Object[]> merchantExpenses = transactionRepository.sumExpensesByMerchant(userId, startDate, endDate);

        if (!merchantExpenses.isEmpty()) {
            Object[] topMerchant = merchantExpenses.get(0);
            String merchantName = (String) topMerchant[0];
            BigDecimal amount = (BigDecimal) topMerchant[1];

            if (merchantName != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                insights.add(InsightDTO.builder()
                        .type("MERCHANT")
                        .title("Top Merchant: " + merchantName)
                        .message(String.format("%s is your highest spending merchant this month with ₹%.2f",
                                merchantName, amount))
                        .severity("info")
                        .build());
            }
        }

        return insights;
    }

    private List<InsightDTO> analyzeWeeklyTrend(Long userId) {
        List<InsightDTO> insights = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findTransactionsForPeriod(userId, startDate, endDate);

        Map<Integer, BigDecimal> weeklySpending = new TreeMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                int weekNumber = t.getDate().get(weekFields.weekOfMonth());
                weeklySpending.merge(weekNumber, t.getAmount(), BigDecimal::add);
            }
        }

        if (weeklySpending.size() >= 2) {
            List<Map.Entry<Integer, BigDecimal>> entries = new ArrayList<>(weeklySpending.entrySet());
            boolean increasing = true;
            for (int i = 1; i < entries.size(); i++) {
                if (entries.get(i).getValue().compareTo(entries.get(i - 1).getValue()) <= 0) {
                    increasing = false;
                    break;
                }
            }

            if (increasing) {
                insights.add(InsightDTO.builder()
                        .type("TREND")
                        .title("Weekly Spending Increasing")
                        .message("Your weekly expenses have been increasing steadily this month. Consider reviewing your spending habits.")
                        .severity("warning")
                        .build());
            }
        }

        return insights;
    }

    private List<InsightDTO> compareWithPreviousMonth(Long userId) {
        List<InsightDTO> insights = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        LocalDate currentStart = currentMonth.atDay(1);
        LocalDate currentEnd = currentMonth.atEndOfMonth();
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();

        BigDecimal currentExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, currentStart, currentEnd);
        BigDecimal previousExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, previousStart, previousEnd);

        if (previousExpense.compareTo(BigDecimal.ZERO) > 0) {
            double changePercent = currentExpense.subtract(previousExpense)
                    .divide(previousExpense, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            if (Math.abs(changePercent) > 20) {
                String message = changePercent > 0
                        ? String.format("Your expenses increased by %.1f%% compared to last month (₹%.2f vs ₹%.2f)",
                                changePercent, currentExpense, previousExpense)
                        : String.format("Great job! Your expenses decreased by %.1f%% compared to last month (₹%.2f vs ₹%.2f)",
                                Math.abs(changePercent), currentExpense, previousExpense);

                insights.add(InsightDTO.builder()
                        .type("COMPARISON")
                        .title("Month-over-Month Comparison")
                        .message(message)
                        .severity(changePercent > 0 ? "warning" : "success")
                        .build());
            }
        }

        return insights;
    }

    private List<InsightDTO> detectSpendingSpikes(Long userId) {
        List<InsightDTO> insights = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findTransactionsForPeriod(userId, startDate, endDate);

        Map<LocalDate, BigDecimal> dailySpending = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                dailySpending.merge(t.getDate(), t.getAmount(), BigDecimal::add);
            }
        }

        if (dailySpending.isEmpty()) {
            return insights;
        }

        BigDecimal avgDaily = dailySpending.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailySpending.size()), 2, RoundingMode.HALF_UP);

        for (Map.Entry<LocalDate, BigDecimal> entry : dailySpending.entrySet()) {
            if (entry.getValue().compareTo(avgDaily.multiply(BigDecimal.valueOf(3))) > 0) {
                insights.add(InsightDTO.builder()
                        .type("SPIKE")
                        .title("Unusual Spending Detected")
                        .message(String.format("You spent ₹%.2f on %s, which is significantly higher than your daily average of ₹%.2f.",
                                entry.getValue(), entry.getKey(), avgDaily))
                        .severity("warning")
                        .build());
                break;
            }
        }

        return insights;
    }
}
