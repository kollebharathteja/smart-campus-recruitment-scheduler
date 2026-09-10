package com.smartcampus.controller;

import com.smartcampus.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/placements")
    public ResponseEntity<Map<String, Object>> placements() {
        return ResponseEntity.ok(reportService.placementSummary());
    }

    @GetMapping("/company-wise")
    public ResponseEntity<Map<String, Object>> companyWise() {
        return ResponseEntity.ok(reportService.companyWise());
    }

    @GetMapping("/department-wise")
    public ResponseEntity<Map<String, Object>> departmentWise() {
        return ResponseEntity.ok(reportService.departmentWise());
    }
}
