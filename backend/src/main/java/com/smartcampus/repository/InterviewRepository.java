package com.smartcampus.repository;

import com.smartcampus.model.Interview;
import com.smartcampus.model.enums.InterviewStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface InterviewRepository extends MongoRepository<Interview, String> {
    List<Interview> findByStudentId(String studentId);
    List<Interview> findByInterviewerIdsContaining(String interviewerId);
    List<Interview> findByPanelIdAndDate(String panelId, LocalDate date);
    List<Interview> findByStudentIdAndDate(String studentId, LocalDate date);
    List<Interview> findByInterviewerIdsContainingAndDate(String interviewerId, LocalDate date);
    List<Interview> findByDriveId(String driveId);
    List<Interview> findByStatus(InterviewStatus status);
    List<Interview> findByDate(LocalDate date);
}
