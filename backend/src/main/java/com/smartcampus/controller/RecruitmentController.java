package com.smartcampus.controller;

import com.smartcampus.dto.EligibilityResultDto;
import com.smartcampus.model.InterviewRound;
import com.smartcampus.model.RecruitmentDrive;
import com.smartcampus.service.EligibilityService;
import com.smartcampus.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final EligibilityService eligibilityService;

    @GetMapping
    public ResponseEntity<List<RecruitmentDrive>> getAll() {
        return ResponseEntity.ok(recruitmentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecruitmentDrive> getById(@PathVariable String id) {
        return ResponseEntity.ok(recruitmentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<RecruitmentDrive> create(@RequestBody RecruitmentDrive drive) {
        return ResponseEntity.ok(recruitmentService.create(drive));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecruitmentDrive> update(@PathVariable String id, @RequestBody RecruitmentDrive drive) {
        return ResponseEntity.ok(recruitmentService.update(id, drive));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recruitmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Dynamic rounds ----
    @PostMapping("/{id}/rounds")
    public ResponseEntity<InterviewRound> addRound(@PathVariable String id, @RequestBody InterviewRound round) {
        round.setDriveId(id);
        return ResponseEntity.ok(recruitmentService.addRound(round));
    }

    @GetMapping("/{id}/rounds")
    public ResponseEntity<List<InterviewRound>> getRounds(@PathVariable String id) {
        return ResponseEntity.ok(recruitmentService.getRounds(id));
    }

    @DeleteMapping("/rounds/{roundId}")
    public ResponseEntity<Void> deleteRound(@PathVariable String roundId) {
        recruitmentService.deleteRound(roundId);
        return ResponseEntity.noContent().build();
    }

    // ---- Eligibility engine ----
    @GetMapping("/{id}/eligible-students")
    public ResponseEntity<List<EligibilityResultDto>> eligibleStudents(@PathVariable String id) {
        return ResponseEntity.ok(eligibilityService.getEligibleStudents(id));
    }

    @GetMapping("/{id}/ineligible-students")
    public ResponseEntity<List<EligibilityResultDto>> ineligibleStudents(@PathVariable String id) {
        return ResponseEntity.ok(eligibilityService.getIneligibleStudents(id));
    }

    @GetMapping("/{id}/eligibility/{studentId}")
    public ResponseEntity<EligibilityResultDto> checkEligibility(@PathVariable String id, @PathVariable String studentId) {
        return ResponseEntity.ok(eligibilityService.evaluateStudentForDrive(studentId, id));
    }
}
