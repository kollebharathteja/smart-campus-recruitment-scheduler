package com.smartcampus.controller;

import com.smartcampus.model.Application;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.service.ApplicationService;
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

    @PostMapping
    public ResponseEntity<Application> apply(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(applicationService.apply(body.get("studentId"), body.get("driveId")));
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
