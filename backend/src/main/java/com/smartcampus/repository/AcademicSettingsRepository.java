package com.smartcampus.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.smartcampus.model.AcademicSettings;

public interface AcademicSettingsRepository extends MongoRepository<AcademicSettings, String> {
}
