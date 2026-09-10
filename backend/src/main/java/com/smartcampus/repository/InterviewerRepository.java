package com.smartcampus.repository;

import com.smartcampus.model.Interviewer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InterviewerRepository extends MongoRepository<Interviewer, String> {
    Optional<Interviewer> findByUserId(String userId);
    Optional<Interviewer> findByEmail(String email);
}
