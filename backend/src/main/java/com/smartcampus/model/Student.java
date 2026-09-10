package com.smartcampus.model;

import com.smartcampus.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "students")
public class Student {

    @Id
    private String id;

    @Indexed
    private String userId; // reference to User._id

    private String name;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true)
    private String rollNumber;

    @Indexed
    private String department;

    @Indexed
    private String degree; // e.g. MCA, B.Tech

    @Field
    @Indexed
    private Double cgpa;

    @Indexed
    private Integer backlogs;

    @Indexed
    private Integer graduationYear;

    @Indexed
    private List<String> skills;

    private String resumeUrl;

    private String phone;

    /**
     * Flexible extra qualification metrics that vary by company — 10th %, 12th %,
     * UG CGPA, PG CGPA, or anything else a drive might require. Key is a free-text
     * label (e.g. "10th Percentage", "UG CGPA") matched against a drive's custom
     * eligibility criteria by the same label.
     */
    @Builder.Default
    private Map<String, Double> additionalDetails = new java.util.HashMap<>();

    /** Overall placement status across all drives: NOT_PLACED / PLACED */
    private ApplicationStatus placementStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
