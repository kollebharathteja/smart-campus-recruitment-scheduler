package com.smartcampus.controller;

import com.smartcampus.model.Interviewer;
import com.smartcampus.security.CurrentUserProvider;
import com.smartcampus.service.InterviewerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviewers")
@RequiredArgsConstructor
public class InterviewerController {

    private final InterviewerService interviewerService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<Interviewer>> getAll() {
        return ResponseEntity.ok(interviewerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Interviewer> getById(@PathVariable String id) {
        return ResponseEntity.ok(interviewerService.getById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<Interviewer> getMyProfile() {
        return ResponseEntity.ok(interviewerService.getByUserId(currentUserProvider.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<Interviewer> create(@RequestBody Interviewer interviewer) {
        return ResponseEntity.ok(interviewerService.create(interviewer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Interviewer> update(@PathVariable String id, @RequestBody Interviewer interviewer) {
        return ResponseEntity.ok(interviewerService.update(id, interviewer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        interviewerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
