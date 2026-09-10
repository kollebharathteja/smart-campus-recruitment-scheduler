package com.smartcampus.model;

import com.smartcampus.model.enums.FeedbackDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feedback")
public class Feedback {

    @Id
    private String id;

    @Indexed
    private String interviewId;

    @Indexed
    private String studentId;

    private String interviewerId;

    private Integer technicalScore;
    private Integer communicationScore;
    private Integer problemSolvingScore;
    private Integer codingScore;
    private Double overallScore;

    private String comments;

    private FeedbackDecision decision;

    private LocalDateTime createdAt;
}
