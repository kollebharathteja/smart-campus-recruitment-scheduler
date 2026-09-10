package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Singleton settings document (fixed id "default") holding the admin-managed
 * master lists of departments and degrees used across the app's forms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "academic_settings")
public class AcademicSettings {

    @Id
    private String id;

    @Builder.Default
    private List<String> departments = new ArrayList<>();

    @Builder.Default
    private List<String> degrees = new ArrayList<>();
}
