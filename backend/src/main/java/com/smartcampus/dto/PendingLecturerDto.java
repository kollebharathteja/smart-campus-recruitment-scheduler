package com.smartcampus.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingLecturerDto {
    private String userId;
    private String name;
    private String email;
    private String department;
    private LocalDateTime requestedAt;
}
