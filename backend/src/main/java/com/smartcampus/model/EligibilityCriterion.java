package com.smartcampus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single admin/lecturer-defined eligibility rule beyond the built-in ones
 * (CGPA, backlogs, degree, department, graduation year, skills). The fieldName
 * must match a key in the student's Student.additionalDetails map — e.g.
 * fieldName "10th Percentage", minimumValue 60.0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCriterion {
    private String fieldName;
    private Double minimumValue;
}
