package com.smartcampus.service;

import com.smartcampus.model.AcademicSettings;
import com.smartcampus.repository.AcademicSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AcademicSettingsService {

    private static final String SETTINGS_ID = "default";

    private final AcademicSettingsRepository repository;

    public AcademicSettings getSettings() {
        return repository.findById(SETTINGS_ID).orElseGet(() -> {
            AcademicSettings defaults = AcademicSettings.builder()
                    .id(SETTINGS_ID)
                    .departments(new ArrayList<>(Arrays.asList("Computer Applications", "Computer Science", "Information Technology")))
                    .degrees(new ArrayList<>(Arrays.asList("MCA", "B.Tech", "M.Tech", "BCA")))
                    .build();
            return repository.save(defaults);
        });
    }

    public AcademicSettings addDepartment(String name) {
        AcademicSettings settings = getSettings();
        String trimmed = name == null ? "" : name.trim();
        if (!trimmed.isEmpty() && settings.getDepartments().stream().noneMatch(d -> d.equalsIgnoreCase(trimmed))) {
            settings.getDepartments().add(trimmed);
            settings = repository.save(settings);
        }
        return settings;
    }

    public AcademicSettings removeDepartment(String name) {
        AcademicSettings settings = getSettings();
        settings.getDepartments().removeIf(d -> d.equalsIgnoreCase(name));
        return repository.save(settings);
    }

    public AcademicSettings addDegree(String name) {
        AcademicSettings settings = getSettings();
        String trimmed = name == null ? "" : name.trim();
        if (!trimmed.isEmpty() && settings.getDegrees().stream().noneMatch(d -> d.equalsIgnoreCase(trimmed))) {
            settings.getDegrees().add(trimmed);
            settings = repository.save(settings);
        }
        return settings;
    }

    public AcademicSettings removeDegree(String name) {
        AcademicSettings settings = getSettings();
        settings.getDegrees().removeIf(d -> d.equalsIgnoreCase(name));
        return repository.save(settings);
    }
}
