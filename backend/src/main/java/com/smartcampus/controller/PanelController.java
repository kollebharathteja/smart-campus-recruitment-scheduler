package com.smartcampus.controller;

import com.smartcampus.model.InterviewPanel;
import com.smartcampus.service.PanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/panels")
@RequiredArgsConstructor
public class PanelController {

    private final PanelService panelService;

    @PostMapping
    public ResponseEntity<InterviewPanel> create(@RequestBody InterviewPanel panel) {
        return ResponseEntity.ok(panelService.create(panel));
    }

    @GetMapping
    public ResponseEntity<List<InterviewPanel>> get(@RequestParam(required = false) String driveId,
                                                      @RequestParam(required = false) String roundId,
                                                      @RequestParam(required = false) String interviewerId) {
        if (driveId != null) return ResponseEntity.ok(panelService.getByDrive(driveId));
        if (roundId != null) return ResponseEntity.ok(panelService.getByRound(roundId));
        if (interviewerId != null) return ResponseEntity.ok(panelService.getByInterviewer(interviewerId));
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewPanel> getById(@PathVariable String id) {
        return ResponseEntity.ok(panelService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewPanel> update(@PathVariable String id, @RequestBody InterviewPanel panel) {
        return ResponseEntity.ok(panelService.update(id, panel));
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<InterviewPanel> rename(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(panelService.rename(id, body.get("panelName")));
    }

    @PostMapping("/{id}/interviewers/{interviewerId}")
    public ResponseEntity<InterviewPanel> addInterviewer(@PathVariable String id, @PathVariable String interviewerId) {
        return ResponseEntity.ok(panelService.addInterviewer(id, interviewerId));
    }

    @DeleteMapping("/{id}/interviewers/{interviewerId}")
    public ResponseEntity<InterviewPanel> removeInterviewer(@PathVariable String id, @PathVariable String interviewerId) {
        return ResponseEntity.ok(panelService.removeInterviewer(id, interviewerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        panelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
