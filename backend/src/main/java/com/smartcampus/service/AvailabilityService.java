package com.smartcampus.service;

import com.smartcampus.exception.InvalidAvailabilityException;
import com.smartcampus.model.Availability;
import com.smartcampus.model.TimeSlot;
import com.smartcampus.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Stores and retrieves STUDENT / INTERVIEWER availability windows that the
 * SchedulerService later intersects to find clash-free interview slots.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    public Availability submit(String ownerId, String ownerType, LocalDate date, List<TimeSlot> slots) {
        validateSlots(slots);

        Availability availability = availabilityRepository
                .findByOwnerIdAndOwnerTypeAndDate(ownerId, ownerType, date)
                .orElse(Availability.builder().ownerId(ownerId).ownerType(ownerType).date(date).build());

        availability.setSlots(slots);
        return availabilityRepository.save(availability);
    }

    private void validateSlots(List<TimeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new InvalidAvailabilityException("At least one availability slot is required.");
        }
        for (TimeSlot slot : slots) {
            if (!slot.isValid()) {
                throw new InvalidAvailabilityException("Slot end time must be after start time.");
            }
        }
        // Reject overlapping slots within the same submission
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                if (slots.get(i).overlaps(slots.get(j))) {
                    throw new InvalidAvailabilityException("Availability slots cannot overlap each other.");
                }
            }
        }
    }

    public List<Availability> getForOwner(String ownerId, String ownerType) {
        return availabilityRepository.findByOwnerIdAndOwnerType(ownerId, ownerType);
    }

    public Availability getForOwnerOnDate(String ownerId, String ownerType, LocalDate date) {
        return availabilityRepository.findByOwnerIdAndOwnerTypeAndDate(ownerId, ownerType, date).orElse(null);
    }
}
