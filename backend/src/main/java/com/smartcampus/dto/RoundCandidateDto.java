package com.smartcampus.dto;

import com.smartcampus.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One student standing in a given round, with their marks if they have already been uploaded. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundCandidateDto {
    private String studentId;
    private String applicationId;
    private String name;
    private String rollNumber;
    private String email;
    private String department;
    private ApplicationStatus applicationStatus;
    private Double marks;
    private Double maxMarks;
    private Double cutoffMarks;
    private Boolean qualified;
    private String remarks;
}
