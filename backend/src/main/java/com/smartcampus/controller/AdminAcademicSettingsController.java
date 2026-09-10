package com.smartcampus.controller;

import com.smartcampus.dto.NameRequest;
import com.smartcampus.model.AcademicSettings;
import com.smartcampus.service.AcademicSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/academic-settings")
@RequiredArgsConstructor
public class AdminAcademicSettingsController {

    private final AcademicSettingsService academicSettingsService;

    @PostMapping("/departments")
    public ResponseEntity<AcademicSettings> addDepartment(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.ok(academicSettingsService.addDepartment(request.getName()));
    }

    @DeleteMapping("/departments/{name}")
    public ResponseEntity<AcademicSettings> removeDepartment(@PathVariable String name) {
        return ResponseEntity.ok(academicSettingsService.removeDepartment(name));
    }

    @PostMapping("/degrees")
    public ResponseEntity<AcademicSettings> addDegree(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.ok(academicSettingsService.addDegree(request.getName()));
    }

    @DeleteMapping("/degrees/{name}")
    public ResponseEntity<AcademicSettings> removeDegree(@PathVariable String name) {
        return ResponseEntity.ok(academicSettingsService.removeDegree(name));
    }
}
