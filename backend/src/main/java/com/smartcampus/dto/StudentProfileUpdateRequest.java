package com.smartcampus.dto;

import lombok.Data;

import java.util.Map;

/**
 * Deliberately narrow — students can only touch their own contact info and
 * self-reported academic history (10th %, 12th %, UG CGPA, PG CGPA, etc.) here.
 * The core eligibility fields (official CGPA, backlogs, department, degree, skills)
 * are admin/lecturer-managed only and never reachable from this DTO.
 */
@Data
public class StudentProfileUpdateRequest {
    private String phone;
    private String resumeUrl;
    private Map<String, Double> additionalDetails;
}
