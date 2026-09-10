package com.smartcampus.model;

import com.smartcampus.model.enums.DriveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recruitmentDrives")
public class RecruitmentDrive {

    @Id
    private String id;

    @Indexed
    private String companyId;

    private String jobRole;

    private String description;

    /** Fully dynamic, configured per drive by the Admin. */
    private RecruitmentRequirements requirements;

    private LocalDate applicationDeadline;

    private LocalDate driveDate;

    @Indexed
    private DriveStatus status;

    private LocalDateTime createdAt;
}
