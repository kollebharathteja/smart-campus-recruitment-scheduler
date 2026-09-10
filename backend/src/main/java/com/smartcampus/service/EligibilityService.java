package com.smartcampus.service;

import com.smartcampus.dto.EligibilityResultDto;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.RecruitmentDrive;
import com.smartcampus.model.RecruitmentRequirements;
import com.smartcampus.model.Student;
import com.smartcampus.repository.RecruitmentDriveRepository;
import com.smartcampus.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Core differentiator #1: dynamic, per-drive eligibility engine.
 *
 * Nothing about the eligibility rules is hard-coded - every drive carries its own
 * RecruitmentRequirements document, and this service evaluates each student against
 * exactly those requirements, returning a precise, human-readable reason for every
 * failure so the student/Admin can see exactly why someone was rejected.
 */
@Service
@RequiredArgsConstructor
public class EligibilityService {

    private final RecruitmentDriveRepository driveRepository;
    private final StudentRepository studentRepository;

    /** Evaluate a single student against a single drive's requirements. */
    public EligibilityResultDto evaluate(Student student, RecruitmentDrive drive) {
        RecruitmentRequirements req = drive.getRequirements();
        List<String> reasons = new ArrayList<>();

        if (req == null) {
            return EligibilityResultDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .eligible(true)
                    .reasons(List.of())
                    .build();
        }

        // --- CGPA check ---
        if (req.getMinimumCgpa() != null) {
            double cgpa = student.getCgpa() == null ? 0.0 : student.getCgpa();
            if (cgpa < req.getMinimumCgpa()) {
                reasons.add(String.format("CGPA is %.1f; minimum required is %.1f", cgpa, req.getMinimumCgpa()));
            }
        }

        // --- Backlogs check ---
        if (req.getMaximumBacklogs() != null) {
            int backlogs = student.getBacklogs() == null ? 0 : student.getBacklogs();
            if (backlogs > req.getMaximumBacklogs()) {
                reasons.add(String.format("Maximum allowed backlogs is %d; student has %d",
                        req.getMaximumBacklogs(), backlogs));
            }
        }

        // --- Degree check ---
        if (req.getEligibleDegrees() != null && !req.getEligibleDegrees().isEmpty()) {
            boolean degreeMatch = req.getEligibleDegrees().stream()
                    .anyMatch(d -> d.equalsIgnoreCase(student.getDegree()));
            if (!degreeMatch) {
                reasons.add(String.format("Degree '%s' is not among the eligible degrees (%s)",
                        student.getDegree(), String.join(", ", req.getEligibleDegrees())));
            }
        }

        // --- Department check (optional - only enforced when the drive restricts departments) ---
        if (req.getEligibleDepartments() != null && !req.getEligibleDepartments().isEmpty()) {
            boolean deptMatch = req.getEligibleDepartments().stream()
                    .anyMatch(d -> d.equalsIgnoreCase(student.getDepartment()));
            if (!deptMatch) {
                reasons.add(String.format("Department '%s' is not eligible for this drive", student.getDepartment()));
            }
        }

        // --- Graduation year check ---
        if (req.getGraduationYear() != null) {
            if (student.getGraduationYear() == null || !student.getGraduationYear().equals(req.getGraduationYear())) {
                reasons.add(String.format("Graduation year must be %d; student's graduation year is %s",
                        req.getGraduationYear(), student.getGraduationYear()));
            }
        }

        // --- Required skills check (partial match allowed via minimumSkillMatchPercent) ---
        if (req.getRequiredSkills() != null && !req.getRequiredSkills().isEmpty()) {
            List<String> studentSkillsLower = student.getSkills() == null ? List.of() :
                    student.getSkills().stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();

            long matchedCount = req.getRequiredSkills().stream()
                    .filter(skill -> studentSkillsLower.contains(skill.toLowerCase(Locale.ROOT)))
                    .count();
            int totalRequired = req.getRequiredSkills().size();
            int matchPercent = totalRequired == 0 ? 100 : (int) Math.round((matchedCount * 100.0) / totalRequired);
            int threshold = req.getMinimumSkillMatchPercent() == null ? 100 : req.getMinimumSkillMatchPercent();

            if (matchPercent < threshold) {
                List<String> missing = req.getRequiredSkills().stream()
                        .filter(skill -> !studentSkillsLower.contains(skill.toLowerCase(Locale.ROOT)))
                        .toList();
                reasons.add(String.format(
                        "Has %d%% of required skills (%s); needs at least %d%%. Missing: %s",
                        matchPercent, matchedCount + "/" + totalRequired, threshold, String.join(", ", missing)));
            }
        }

        // --- Custom eligibility criteria (e.g. 10th %, 12th %, UG CGPA, PG CGPA) ---
        if (req.getCustomCriteria() != null) {
            java.util.Map<String, Double> details = student.getAdditionalDetails() == null
                    ? java.util.Map.of() : student.getAdditionalDetails();
            for (com.smartcampus.model.EligibilityCriterion criterion : req.getCustomCriteria()) {
                if (criterion.getFieldName() == null || criterion.getMinimumValue() == null) continue;
                Double value = details.get(criterion.getFieldName());
                if (value == null) {
                    reasons.add(String.format("Missing required detail \"%s\"", criterion.getFieldName()));
                } else if (value < criterion.getMinimumValue()) {
                    reasons.add(String.format("%s is %.1f; minimum required is %.1f",
                            criterion.getFieldName(), value, criterion.getMinimumValue()));
                }
            }
        }

        boolean eligible = reasons.isEmpty();

        return EligibilityResultDto.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .eligible(eligible)
                .reasons(reasons)
                .build();
    }

    public EligibilityResultDto evaluateStudentForDrive(String studentId, String driveId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        RecruitmentDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruitment drive not found: " + driveId));
        return evaluate(student, drive);
    }

    /** Evaluate every student in the system against a drive - used to build eligible/ineligible lists. */
    public List<EligibilityResultDto> evaluateAllForDrive(String driveId) {
        RecruitmentDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruitment drive not found: " + driveId));

        return studentRepository.findAll().stream()
                .map(student -> evaluate(student, drive))
                .toList();
    }

    public List<EligibilityResultDto> getEligibleStudents(String driveId) {
        return evaluateAllForDrive(driveId).stream().filter(EligibilityResultDto::isEligible).toList();
    }

    public List<EligibilityResultDto> getIneligibleStudents(String driveId) {
        return evaluateAllForDrive(driveId).stream().filter(r -> !r.isEligible()).toList();
    }
}
