package com.fintrac.controller;

import com.fintrac.dto.InsightDTO;
import com.fintrac.dto.MonthlyReportDTO;
import com.fintrac.service.InsightService;
import com.fintrac.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final InsightService insightService;

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportDTO> getMonthlyReport(
            @PathVariable int year, @PathVariable int month) {
        MonthlyReportDTO report = reportService.getMonthlyReport(year, month);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/monthly/current")
    public ResponseEntity<MonthlyReportDTO> getCurrentMonthReport() {
        java.time.YearMonth current = java.time.YearMonth.now();
        MonthlyReportDTO report = reportService.getMonthlyReport(current.getYear(), current.getMonthValue());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/quarterly/{year}/{quarter}")
    public ResponseEntity<List<MonthlyReportDTO>> getQuarterlyReport(
            @PathVariable int year, @PathVariable int quarter) {
        List<MonthlyReportDTO> reports = reportService.getQuarterlyReport(year, quarter);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/insights")
    public ResponseEntity<List<InsightDTO>> getInsights() {
        List<InsightDTO> insights = insightService.generateInsights(null);
        return ResponseEntity.ok(insights);
    }
}
