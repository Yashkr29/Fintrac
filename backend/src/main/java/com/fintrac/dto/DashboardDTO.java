package com.fintrac.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal remainingBudget;
    private BigDecimal savings;
    private Double budgetUsagePercentage;
    private String budgetStatus;
    private List<AlertDTO> recentAlerts;
    private List<InsightDTO> insights;
    private Map<String, BigDecimal> categoryBreakdown;
    private Map<String, BigDecimal> weeklyTrend;
}
