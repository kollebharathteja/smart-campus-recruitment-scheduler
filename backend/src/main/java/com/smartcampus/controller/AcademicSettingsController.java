package com.smartcampus.controller;

import com.smartcampus.model.AcademicSettings;
import com.smartcampus.service.AcademicSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic-settings")
@RequiredArgsConstructor
public class AcademicSettingsController {

    private final AcademicSettingsService academicSettingsService;

    @GetMapping
    public ResponseEntity<AcademicSettings> get() {
        return ResponseEntity.ok(academicSettingsService.getSettings());
    }
}
