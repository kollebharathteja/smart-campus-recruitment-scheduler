package com.smartcampus.repository;

import com.smartcampus.model.InterviewRound;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InterviewRoundRepository extends MongoRepository<InterviewRound, String> {
    List<InterviewRound> findByDriveIdOrderBySequenceAsc(String driveId);
}
