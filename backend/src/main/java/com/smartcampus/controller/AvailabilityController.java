package com.smartcampus.controller;

import com.smartcampus.dto.AvailabilityRequest;
import com.smartcampus.model.Availability;
import com.smartcampus.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping("/student")
    public ResponseEntity<Availability> submitStudent(@RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.submit(request.getOwnerId(), "STUDENT", request.getDate(), request.getSlots()));
    }

    @PostMapping("/interviewer")
    public ResponseEntity<Availability> submitInterviewer(@RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.submit(request.getOwnerId(), "INTERVIEWER", request.getDate(), request.getSlots()));
    }

    @GetMapping("/student/{id}")
    public ResponseEntity<List<Availability>> getStudent(@PathVariable String id) {
        return ResponseEntity.ok(availabilityService.getForOwner(id, "STUDENT"));
    }

    @GetMapping("/interviewer/{id}")
    public ResponseEntity<List<Availability>> getInterviewer(@PathVariable String id) {
        return ResponseEntity.ok(availabilityService.getForOwner(id, "INTERVIEWER"));
    }
}
