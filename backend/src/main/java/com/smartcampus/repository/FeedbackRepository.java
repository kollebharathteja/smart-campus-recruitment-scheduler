package com.smartcampus.repository;

import com.smartcampus.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByInterviewId(String interviewId);
    List<Feedback> findByStudentId(String studentId);
    Optional<Feedback> findByInterviewIdAndInterviewerId(String interviewId, String interviewerId);
}
