package com.fintrac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDTO {
    private Long id;

    @NotBlank(message = "Month is required (format: YYYY-MM)")
    private String month;

    @NotNull(message = "Initial budget is required")
    @Positive(message = "Initial budget must be positive")
    private BigDecimal initialBudget;

    private BigDecimal adjustedBudget;
    private BigDecimal emergencySpent;
    private BigDecimal totalSpent;
    private BigDecimal remainingBudget;
    private Double usagePercentage;
    private String status;
}
