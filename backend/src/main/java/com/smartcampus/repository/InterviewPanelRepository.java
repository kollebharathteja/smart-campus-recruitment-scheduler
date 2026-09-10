package com.smartcampus.repository;

import com.smartcampus.model.InterviewPanel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InterviewPanelRepository extends MongoRepository<InterviewPanel, String> {
    List<InterviewPanel> findByDriveId(String driveId);
    List<InterviewPanel> findByRoundId(String roundId);
    List<InterviewPanel> findByInterviewerIdsContaining(String interviewerId);
}
