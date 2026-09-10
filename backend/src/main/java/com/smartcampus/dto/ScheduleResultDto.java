package com.smartcampus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResultDto {
    private List<String> scheduledInterviewIds;
    private List<UnscheduledCandidateDto> unscheduled;
}
