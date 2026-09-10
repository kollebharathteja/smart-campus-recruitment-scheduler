package com.smartcampus.repository;

import com.smartcampus.model.Availability;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends MongoRepository<Availability, String> {
    List<Availability> findByOwnerIdAndOwnerType(String ownerId, String ownerType);
    Optional<Availability> findByOwnerIdAndOwnerTypeAndDate(String ownerId, String ownerType, LocalDate date);
    List<Availability> findByOwnerIdInAndOwnerTypeAndDate(List<String> ownerIds, String ownerType, LocalDate date);
}
