package com.smartcampus.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded document inside RecruitmentDrive. Fully dynamic per drive -
 * nothing here is hard-coded in the eligibility engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentRequirements {

    private Double minimumCgpa;

    private Integer maximumBacklogs;

    private List<String> eligibleDegrees;

    private List<String> eligibleDepartments; // empty/null = all departments allowed

    private Integer graduationYear; // null = any year

    private List<String> requiredSkills;

    /**
     * What fraction of requiredSkills a student must have, 0-100. Null or 100 means
     * every required skill is mandatory; e.g. 50 means at least half is enough.
     */
    private Integer minimumSkillMatchPercent;

    /** Admin/lecturer-defined extra rules beyond the built-in fields (10th %, UG CGPA, etc). */
    private List<EligibilityCriterion> customCriteria;
}
