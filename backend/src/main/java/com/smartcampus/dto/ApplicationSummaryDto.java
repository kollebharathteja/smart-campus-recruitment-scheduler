package com.smartcampus.dto;

import com.smartcampus.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything a student needs to see about one of their drive applications in one call:
 * which company/role, current round, status, and — once interviews have happened —
 * the round-by-round marks/feedback. Built for the student dashboard so the frontend
 * doesn't have to stitch together Application + Drive + Company + Round + Interview +
 * Feedback itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryDto {
    private String applicationId;
    private String driveId;
    private String companyName;
    private String jobRole;
    private ApplicationStatus status;
    private String currentRoundName;
    private Integer currentRoundSequence;
    private LocalDateTime shortlistedAt;
    private List<String> eligibilityReasons;
    private List<RoundResultDto> roundResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundResultDto {
        private String roundId;
        private String roundName;
        private Integer sequence;
        private String panelName;
        private String interviewStatus; // SCHEDULED / COMPLETED / ... or null if not scheduled yet
        private java.time.LocalDate interviewDate;
        private String venue;
        private String meetingLink;
        private Double overallScore;
        private String decision; // SELECT / REJECT / HOLD, from Feedback, if graded
        private String comments;
    }
}
