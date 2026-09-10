package com.smartcampus.dto;

import com.smartcampus.model.enums.SchedulingStrategy;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleRequest {
    private String driveId;
    private String roundId;
    /** Optional: restrict scheduling to specific applicationIds. If empty, all SHORTLISTED/IN_PROCESS candidates for the round are scheduled. */
    private List<String> applicationIds;
    private List<LocalDate> candidateDates; // dates the scheduler is allowed to search
    private Integer durationMinutes; // overrides round's default duration if provided
    private SchedulingStrategy strategy = SchedulingStrategy.FIRST_AVAILABLE;
}
