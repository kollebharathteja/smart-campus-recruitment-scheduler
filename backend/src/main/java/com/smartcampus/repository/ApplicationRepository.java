package com.smartcampus.repository;

import com.smartcampus.model.Application;
import com.smartcampus.model.enums.ApplicationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    List<Application> findByDriveId(String driveId);
    List<Application> findByStudentId(String studentId);
    Optional<Application> findByStudentIdAndDriveId(String studentId, String driveId);
    List<Application> findByDriveIdAndStatus(String driveId, ApplicationStatus status);
    boolean existsByStudentIdAndDriveId(String studentId, String driveId);
}
