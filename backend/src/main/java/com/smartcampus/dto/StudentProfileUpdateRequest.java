package com.smartcampus.dto;

import lombok.Data;

/**
 * Deliberately narrow — students can only touch their own contact/basic info here.
 * Everything eligibility-relevant (CGPA, backlogs, department, degree, skills,
 * additionalDetails) is admin/lecturer-managed only and never reachable from this DTO.
 */
@Data
public class StudentProfileUpdateRequest {
    private String phone;
    private String resumeUrl;
}
