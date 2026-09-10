package com.smartcampus.dto;

import com.smartcampus.model.enums.FeedbackDecision;
import lombok.Data;

@Data
public class FeedbackRequest {
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer problemSolvingScore;
    private Integer codingScore;
    private String comments;
    private FeedbackDecision decision;
}
