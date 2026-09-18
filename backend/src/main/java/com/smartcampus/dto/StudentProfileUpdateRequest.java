package com.smartcampus.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Deliberately narrow — students can only touch their own contact info, skills and
 * self-reported academic history (10th %, 12th %, UG CGPA, etc.) here. The core
 * eligibility fields (official CGPA, backlogs, department, degree) are admin/lecturer-managed
 * only and never reachable from this DTO.
 */
@Data
public class StudentProfileUpdateRequest {
    private String phone;
    private String resumeUrl;
    private List<String> skills;
    private Map<String, Double> additionalDetails;
}
