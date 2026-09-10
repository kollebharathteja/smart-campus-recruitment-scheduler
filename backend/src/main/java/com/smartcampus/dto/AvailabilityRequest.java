package com.smartcampus.dto;

import com.smartcampus.model.TimeSlot;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AvailabilityRequest {
    private String ownerId; // studentId or interviewerId; if omitted, resolved from logged-in user
    private LocalDate date;
    private List<TimeSlot> slots;
}
