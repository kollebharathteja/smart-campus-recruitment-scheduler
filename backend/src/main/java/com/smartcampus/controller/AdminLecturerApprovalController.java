package com.smartcampus.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcampus.dto.PendingLecturerDto;
import com.smartcampus.service.InterviewerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/lecturer-approvals")
@RequiredArgsConstructor
public class AdminLecturerApprovalController {

    private final InterviewerService interviewerService;

    @GetMapping
    public ResponseEntity<List<PendingLecturerDto>> getPending() {
        return ResponseEntity.ok(interviewerService.getPendingLecturers());
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<Void> approve(@PathVariable String userId) {
        interviewerService.approveLecturer(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<Void> reject(@PathVariable String userId) {
        interviewerService.rejectLecturer(userId);
        return ResponseEntity.ok().build();
    }
}
