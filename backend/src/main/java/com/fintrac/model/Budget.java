package com.fintrac.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(name = "initial_budget", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal initialBudget = BigDecimal.ZERO;

    @Column(name = "adjusted_budget", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal adjustedBudget = BigDecimal.ZERO;

    @Column(name = "emergency_spent", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal emergencySpent = BigDecimal.ZERO;

    @Column(name = "total_spent", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (adjustedBudget == null || adjustedBudget.compareTo(BigDecimal.ZERO) == 0) {
            adjustedBudget = initialBudget;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
