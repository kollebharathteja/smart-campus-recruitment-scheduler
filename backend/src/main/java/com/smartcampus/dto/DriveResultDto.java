package com.smartcampus.dto;

import com.smartcampus.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Round-by-round outcome for one student in one drive — what the admin's results view and a
 * lecturer's department view both render. {@code status} is the live application status, so
 * SELECTED means the student cleared the final round.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriveResultDto {
    private String studentId;
    private String applicationId;
    private String studentName;
    private String rollNumber;
    private String email;
    private String department;
    private String driveId;
    private String companyName;
    private String jobRole;
    private ApplicationStatus status;
    private String currentRoundName;
    private List<RoundMarkDto> rounds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundMarkDto {
        private String roundId;
        private String roundName;
        private Integer sequence;
        private Double marks;
        private Double maxMarks;
        private Double cutoffMarks;
        private Boolean qualified;
        private String remarks;
    }
}
