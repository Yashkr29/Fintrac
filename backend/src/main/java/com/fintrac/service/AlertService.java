package com.fintrac.service;

import com.fintrac.dto.AlertDTO;
import com.fintrac.dto.BudgetDTO;
import com.fintrac.model.Alert;
import com.fintrac.model.AlertType;
import com.fintrac.model.User;
import com.fintrac.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AuthService authService;
    private final BudgetService budgetService;

    private static final double WARNING_THRESHOLD = 80.0;
    private static final double CRITICAL_THRESHOLD = 100.0;

    @Transactional(readOnly = true)
    public List<AlertDTO> getUnreadAlerts() {
        Long userId = authService.getCurrentUserId();
        return alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> getAllAlerts() {
        Long userId = authService.getCurrentUserId();
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        Long userId = authService.getCurrentUserId();
        return alertRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAllAsRead() {
        Long userId = authService.getCurrentUserId();
        alertRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void checkBudgetAlerts(Long userId) {
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BudgetDTO budget = budgetService.getBudgetByMonth(currentMonth);

        if (budget.getAdjustedBudget() == null || budget.getAdjustedBudget().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        double usagePercentage = budget.getUsagePercentage();

        if (usagePercentage >= CRITICAL_THRESHOLD) {
            createAlert(userId, AlertType.CRITICAL,
                    "Budget Exceeded!",
                    String.format("You have exceeded your budget for %s. You've spent ₹%.2f out of ₹%.2f (%.1f%%). Consider reviewing your expenses.",
                            currentMonth, budget.getTotalSpent(), budget.getAdjustedBudget(), usagePercentage));
        } else if (usagePercentage >= WARNING_THRESHOLD) {
            createAlert(userId, AlertType.WARNING,
                    "Budget Warning",
                    String.format("You have used %.1f%% of your budget for %s. You have ₹%.2f remaining.",
                            usagePercentage, currentMonth, budget.getRemainingBudget()));
        }
    }

    @Transactional
    public void createAlert(Long userId, AlertType type, String title, String message) {
        List<Alert> existingAlerts = alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        boolean alertExists = existingAlerts.stream()
                .anyMatch(a -> a.getTitle().equals(title) &&
                        a.getCreatedAt().toLocalDate().equals(LocalDate.now()));

        if (!alertExists) {
            User user = User.builder().id(userId).build();
            Alert alert = Alert.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .build();
            alertRepository.save(alert);
        }
    }

    private AlertDTO toDTO(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .type(alert.getType())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .isRead(alert.getIsRead())
                .build();
    }
}
