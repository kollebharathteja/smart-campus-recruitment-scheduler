package com.smartcampus.controller;

import com.smartcampus.dto.ScheduleRequest;
import com.smartcampus.dto.ScheduleResultDto;
import com.smartcampus.model.Interview;
import com.smartcampus.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerService schedulerService;

    /** Core differentiator endpoint: automated clash-free scheduling for a round. */
    @PostMapping("/generate")
    public ResponseEntity<ScheduleResultDto> generate(@RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(schedulerService.generateSchedule(request));
    }

    @PostMapping("/reschedule")
    public ResponseEntity<Interview> reschedule(@RequestBody Map<String, Object> body) {
        String interviewId = (String) body.get("interviewId");
        @SuppressWarnings("unchecked")
        List<String> dateStrings = (List<String>) body.get("candidateDates");
        List<LocalDate> dates = dateStrings.stream().map(LocalDate::parse).toList();
        return ResponseEntity.ok(schedulerService.reschedule(interviewId, dates));
    }

    @GetMapping("/unscheduled")
    public ResponseEntity<List<Interview>> unscheduled() {
        // Unscheduled candidates are returned directly in the response of /generate.
        // This endpoint is kept for API completeness / future caching.
        return ResponseEntity.ok(schedulerService.getUnscheduledPlaceholder());
    }
}
