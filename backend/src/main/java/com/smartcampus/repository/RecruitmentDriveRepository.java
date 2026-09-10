package com.smartcampus.repository;

import com.smartcampus.model.RecruitmentDrive;
import com.smartcampus.model.enums.DriveStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RecruitmentDriveRepository extends MongoRepository<RecruitmentDrive, String> {
    List<RecruitmentDrive> findByCompanyId(String companyId);
    List<RecruitmentDrive> findByStatus(DriveStatus status);
}
