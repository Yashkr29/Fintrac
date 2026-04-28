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
public class MonthlyReportDTO {
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal savings;
    private BigDecimal budgetVsActual;
    private Double savingsRate;
    private Map<String, BigDecimal> categoryWiseBreakdown;
    private Map<String, BigDecimal> merchantWiseSpending;
    private List<WeeklySummaryDTO> weeklySummaries;
    private List<InsightDTO> insights;
}
