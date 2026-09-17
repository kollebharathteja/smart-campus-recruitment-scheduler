package com.smartcampus.controller;

import com.smartcampus.dto.ApplicationSummaryDto;
import com.smartcampus.model.Application;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.security.CurrentUserProvider;
import com.smartcampus.service.ApplicationService;
import com.smartcampus.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final StudentService studentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<Application> apply(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(applicationService.apply(body.get("studentId"), body.get("driveId")));
    }

    /**
     * Enriched, per-drive view of the logged-in student's own applications — company, role,
     * current round, status, and marks/feedback for every round completed so far. Powers the
     * "My Applications" section of the student dashboard.
     */
    @GetMapping("/me/summary")
    public ResponseEntity<List<ApplicationSummaryDto>> mySummary() {
        String studentId = studentService.getByUserId(currentUserProvider.getCurrentUserId()).getId();
        return ResponseEntity.ok(applicationService.getSummaryForStudent(studentId));
    }

    @GetMapping
    public ResponseEntity<List<Application>> get(@RequestParam(required = false) String driveId,
                                                   @RequestParam(required = false) String studentId) {
        if (driveId != null) return ResponseEntity.ok(applicationService.getByDrive(driveId));
        if (studentId != null) return ResponseEntity.ok(applicationService.getByStudent(studentId));
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable String id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Application> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(applicationService.updateStatus(id, ApplicationStatus.valueOf(body.get("status"))));
    }

    @PostMapping("/{id}/shortlist")
    public ResponseEntity<Application> shortlist(@PathVariable String id) {
        return ResponseEntity.ok(applicationService.shortlist(id));
    }

    @PostMapping("/shortlist-all/{driveId}")
    public ResponseEntity<List<Application>> shortlistAll(@PathVariable String driveId) {
        return ResponseEntity.ok(applicationService.shortlistAllEligible(driveId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Application> reject(@PathVariable String id) {
        return ResponseEntity.ok(applicationService.reject(id));
    }
}
