package com.smartcampus.repository;

import com.smartcampus.model.RoundResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoundResultRepository extends MongoRepository<RoundResult, String> {
    List<RoundResult> findByRoundId(String roundId);
    List<RoundResult> findByDriveId(String driveId);
    List<RoundResult> findByStudentId(String studentId);
    List<RoundResult> findByDriveIdAndStudentId(String driveId, String studentId);
    Optional<RoundResult> findByRoundIdAndStudentId(String roundId, String studentId);
}
