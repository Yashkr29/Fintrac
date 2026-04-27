package com.fintrac.controller;

import com.fintrac.dto.BudgetDTO;
import com.fintrac.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/current")
    public ResponseEntity<BudgetDTO> getCurrentMonthBudget() {
        BudgetDTO budget = budgetService.getCurrentMonthBudget();
        return ResponseEntity.ok(budget);
    }

    @GetMapping("/{month}")
    public ResponseEntity<BudgetDTO> getBudgetByMonth(@PathVariable String month) {
        BudgetDTO budget = budgetService.getBudgetByMonth(month);
        return ResponseEntity.ok(budget);
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createOrUpdateBudget(@Valid @RequestBody BudgetDTO budgetDTO) {
        BudgetDTO budget = budgetService.createOrUpdateBudget(budgetDTO);
        return ResponseEntity.ok(budget);
    }

    @GetMapping("/{month}/daily-remaining")
    public ResponseEntity<BigDecimal> getDailyBudgetRemaining(@PathVariable String month) {
        BigDecimal dailyRemaining = budgetService.calculateDailyBudgetRemaining(month);
        return ResponseEntity.ok(dailyRemaining);
    }

    @GetMapping("/{month}/status")
    public ResponseEntity<Map<String, String>> getBudgetStatus(@PathVariable String month) {
        String status = budgetService.getBudgetStatus(null, month);
        return ResponseEntity.ok(Map.of("status", status));
    }
}
