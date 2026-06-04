package com.fintrac.service;

import com.fintrac.dto.BudgetDTO;
import com.fintrac.exception.ResourceNotFoundException;
import com.fintrac.model.Budget;
import com.fintrac.model.Transaction;
import com.fintrac.model.TransactionType;
import com.fintrac.model.User;
import com.fintrac.repository.BudgetRepository;
import com.fintrac.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    private static final double WARNING_THRESHOLD = 0.80;
    private static final double CRITICAL_THRESHOLD = 1.00;

    @Transactional(readOnly = true)
    public BudgetDTO getBudgetByMonth(String month) {
        Long userId = authService.getCurrentUserId();
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(Budget.builder()
                        .user(User.builder().id(userId).build())
                        .month(month)
                        .initialBudget(BigDecimal.ZERO)
                        .adjustedBudget(BigDecimal.ZERO)
                        .emergencySpent(BigDecimal.ZERO)
                        .totalSpent(BigDecimal.ZERO)
                        .build());
        return toDTO(budget);
    }

    @Transactional(readOnly = true)
    public BudgetDTO getCurrentMonthBudget() {
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return getBudgetByMonth(currentMonth);
    }

    @Transactional
    public BudgetDTO createOrUpdateBudget(BudgetDTO budgetDTO) {
        Long userId = authService.getCurrentUserId();
        User currentUser = User.builder().id(userId).build();

        Budget budget = budgetRepository.findByUserIdAndMonth(userId, budgetDTO.getMonth())
                .orElse(Budget.builder()
                        .user(currentUser)
                        .month(budgetDTO.getMonth())
                        .build());

        budget.setInitialBudget(budgetDTO.getInitialBudget());
        recalculateBudgetTotals(userId, budget);

        Budget saved = budgetRepository.save(budget);
        return toDTO(saved);
    }

    @Transactional
    public void updateBudgetSpending(Long userId, LocalDate transactionDate, BigDecimal amount, boolean isEmergency) {
        String month = transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(null);

        if (budget == null) {
            return;
        }

        budget.setTotalSpent(budget.getTotalSpent().add(amount));

        if (isEmergency) {
            budget.setEmergencySpent(budget.getEmergencySpent().add(amount));
            adaptBudgetForEmergency(budget, amount);
        }

        budgetRepository.save(budget);
    }

    @Transactional
    public void recalculateBudgetSpending(Long userId, LocalDate transactionDate,
                                          BigDecimal oldAmount, boolean oldWasEmergency,
                                          BigDecimal newAmount, boolean newIsEmergency) {
        String month = transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(null);

        if (budget == null) {
            return;
        }

        budget.setTotalSpent(budget.getTotalSpent().subtract(oldAmount));

        if (oldWasEmergency) {
            budget.setEmergencySpent(budget.getEmergencySpent().subtract(oldAmount));
            reverseEmergencyAdaptation(budget, oldAmount);
        }

        budget.setTotalSpent(budget.getTotalSpent().add(newAmount));

        if (newIsEmergency) {
            budget.setEmergencySpent(budget.getEmergencySpent().add(newAmount));
            adaptBudgetForEmergency(budget, newAmount);
        }

        budgetRepository.save(budget);
    }

    @Transactional
    public void recalculateBudgetOnDelete(Long userId, LocalDate transactionDate,
                                          BigDecimal amount, boolean wasEmergency) {
        String month = transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(null);

        if (budget == null) {
            return;
        }

        budget.setTotalSpent(budget.getTotalSpent().subtract(amount));

        if (wasEmergency) {
            budget.setEmergencySpent(budget.getEmergencySpent().subtract(amount));
            reverseEmergencyAdaptation(budget, amount);
        }

        budgetRepository.save(budget);
    }

    private void adaptBudgetForEmergency(Budget budget, BigDecimal emergencyAmount) {
        BigDecimal newAdjusted = budget.getAdjustedBudget().add(emergencyAmount);
        budget.setAdjustedBudget(newAdjusted);
    }

    private void reverseEmergencyAdaptation(Budget budget, BigDecimal amount) {
        BigDecimal newAdjusted = budget.getAdjustedBudget().subtract(amount);
        if (newAdjusted.compareTo(budget.getInitialBudget()) < 0) {
            newAdjusted = budget.getInitialBudget();
        }
        budget.setAdjustedBudget(newAdjusted);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDailyBudgetRemaining(String month) {
        Long userId = authService.getCurrentUserId();
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(null);

        if (budget == null || budget.getAdjustedBudget().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate today = LocalDate.now();
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        long totalDays = yearMonth.lengthOfMonth();
        long remainingDays = ChronoUnit.DAYS.between(today, endOfMonth) + 1;

        if (remainingDays <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal spent = budget.getTotalSpent();
        BigDecimal adjusted = budget.getAdjustedBudget();
        BigDecimal remaining = adjusted.subtract(spent);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return remaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public String getBudgetStatus(Long userId, String month) {
        if (userId == null) {
            userId = authService.getCurrentUserId();
        }
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(null);

        if (budget == null || budget.getAdjustedBudget().compareTo(BigDecimal.ZERO) == 0) {
            return "NO_BUDGET";
        }

        double usagePercentage = budget.getTotalSpent()
                .divide(budget.getAdjustedBudget(), 4, RoundingMode.HALF_UP)
                .doubleValue();

        if (usagePercentage >= CRITICAL_THRESHOLD) {
            return "EXCEEDED";
        } else if (usagePercentage >= WARNING_THRESHOLD) {
            return "WARNING";
        } else {
            return "GOOD";
        }
    }

    private void recalculateBudgetTotals(Long userId, Budget budget) {
        YearMonth yearMonth = YearMonth.parse(budget.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal totalSpent = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, startDate, endDate);

        BigDecimal emergencySpent = transactionRepository.findEmergencyExpenses(userId, startDate, endDate)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        budget.setTotalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO);
        budget.setEmergencySpent(emergencySpent);
        budget.setAdjustedBudget(budget.getInitialBudget().add(emergencySpent));
    }

    private BudgetDTO toDTO(Budget budget) {
        BigDecimal remaining = BigDecimal.ZERO;
        Double usagePercentage = 0.0;
        String status = "NO_BUDGET";

        if (budget != null && budget.getAdjustedBudget() != null
                && budget.getAdjustedBudget().compareTo(BigDecimal.ZERO) > 0) {
            remaining = budget.getAdjustedBudget().subtract(budget.getTotalSpent());
            usagePercentage = budget.getTotalSpent()
                    .divide(budget.getAdjustedBudget(), 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;

            if (usagePercentage >= 100) {
                status = "EXCEEDED";
            } else if (usagePercentage >= 80) {
                status = "WARNING";
            } else {
                status = "GOOD";
            }
        }

        return BudgetDTO.builder()
                .id(budget != null ? budget.getId() : null)
                .month(budget != null ? budget.getMonth() : null)
                .initialBudget(budget != null ? budget.getInitialBudget() : BigDecimal.ZERO)
                .adjustedBudget(budget != null ? budget.getAdjustedBudget() : BigDecimal.ZERO)
                .emergencySpent(budget != null ? budget.getEmergencySpent() : BigDecimal.ZERO)
                .totalSpent(budget != null ? budget.getTotalSpent() : BigDecimal.ZERO)
                .remainingBudget(remaining)
                .usagePercentage(usagePercentage)
                .status(status)
                .isEmergency(budget != null && budget.getEmergencySpent() != null
                        && budget.getEmergencySpent().compareTo(BigDecimal.ZERO) > 0)
                .build();
    }
}
