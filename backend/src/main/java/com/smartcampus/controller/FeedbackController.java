package com.smartcampus.controller;

import com.smartcampus.dto.FeedbackRequest;
import com.smartcampus.model.Feedback;
import com.smartcampus.security.CurrentUserProvider;
import com.smartcampus.service.FeedbackService;
import com.smartcampus.service.InterviewerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews/{interviewId}/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final InterviewerService interviewerService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<Feedback> submit(@PathVariable String interviewId, @RequestBody FeedbackRequest request) {
        String interviewerId = interviewerService.getByUserId(currentUserProvider.getCurrentUserId()).getId();
        return ResponseEntity.ok(feedbackService.submit(interviewId, interviewerId, request));
    }

    @GetMapping
    public ResponseEntity<List<Feedback>> get(@PathVariable String interviewId) {
        return ResponseEntity.ok(feedbackService.getForInterview(interviewId));
    }
}
