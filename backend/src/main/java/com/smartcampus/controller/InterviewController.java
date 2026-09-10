package com.smartcampus.controller;

import com.smartcampus.model.Interview;
import com.smartcampus.model.enums.InterviewStatus;
import com.smartcampus.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping
    public ResponseEntity<List<Interview>> get(@RequestParam(required = false) String studentId,
                                                 @RequestParam(required = false) String interviewerId,
                                                 @RequestParam(required = false) String driveId) {
        if (studentId != null) return ResponseEntity.ok(interviewService.getForStudent(studentId));
        if (interviewerId != null) return ResponseEntity.ok(interviewService.getForInterviewer(interviewerId));
        if (driveId != null) return ResponseEntity.ok(interviewService.getForDrive(driveId));
        return ResponseEntity.ok(interviewService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Interview> getById(@PathVariable String id) {
        return ResponseEntity.ok(interviewService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Interview> updateDetails(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(interviewService.updateDetails(id, body.get("venue"), body.get("meetingLink")));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Interview> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(interviewService.updateStatus(id, InterviewStatus.valueOf(body.get("status"))));
    }
}
