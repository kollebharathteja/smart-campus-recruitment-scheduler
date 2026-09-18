package com.smartcampus.controller;

import com.smartcampus.dto.DriveResultDto;
import com.smartcampus.dto.RoundCandidateDto;
import com.smartcampus.dto.RoundMarksUploadRequest;
import com.smartcampus.model.RoundResult;
import com.smartcampus.service.RoundResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin/lecturer endpoints for grading a round and reading the resulting selections. */
@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RoundResultController {

    private final RoundResultService roundResultService;

    @GetMapping("/{roundId}/candidates")
    public ResponseEntity<List<RoundCandidateDto>> candidates(@PathVariable String roundId) {
        return ResponseEntity.ok(roundResultService.getCandidates(roundId));
    }

    @PostMapping("/{roundId}/marks")
    public ResponseEntity<List<RoundResult>> uploadMarks(@PathVariable String roundId,
                                                         @RequestBody RoundMarksUploadRequest request) {
        return ResponseEntity.ok(roundResultService.uploadMarks(roundId, request));
    }

    @GetMapping("/{roundId}/results")
    public ResponseEntity<List<RoundResult>> results(@PathVariable String roundId) {
        return ResponseEntity.ok(roundResultService.getResults(roundId));
    }

    @GetMapping("/drives/{driveId}/results")
    public ResponseEntity<List<DriveResultDto>> driveResults(@PathVariable String driveId) {
        return ResponseEntity.ok(roundResultService.getDriveResults(driveId));
    }

    /** Every drive, scoped to the caller: a lecturer sees only their own department's students. */
    @GetMapping("/results")
    public ResponseEntity<List<DriveResultDto>> allResults() {
        return ResponseEntity.ok(roundResultService.getAllResults());
    }
}
