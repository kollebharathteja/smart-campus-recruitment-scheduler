package com.smartcampus.service;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.InterviewRound;
import com.smartcampus.model.RecruitmentDrive;
import com.smartcampus.repository.InterviewRoundRepository;
import com.smartcampus.repository.RecruitmentDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final RecruitmentDriveRepository driveRepository;
    private final InterviewRoundRepository roundRepository;

    public List<RecruitmentDrive> getAll() {
        return driveRepository.findAll();
    }

    public RecruitmentDrive getById(String id) {
        return driveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruitment drive not found: " + id));
    }

    public RecruitmentDrive create(RecruitmentDrive drive) {
        drive.setCreatedAt(LocalDateTime.now());
        return driveRepository.save(drive);
    }

    public RecruitmentDrive update(String id, RecruitmentDrive updates) {
        RecruitmentDrive existing = getById(id);
        existing.setJobRole(updates.getJobRole());
        existing.setDescription(updates.getDescription());
        existing.setRequirements(updates.getRequirements());
        existing.setApplicationDeadline(updates.getApplicationDeadline());
        existing.setDriveDate(updates.getDriveDate());
        existing.setStatus(updates.getStatus());
        return driveRepository.save(existing);
    }

    public void delete(String id) {
        driveRepository.deleteById(id);
    }

    // ---- Dynamic interview round management (per drive) ----

    public InterviewRound addRound(InterviewRound round) {
        return roundRepository.save(round);
    }

    public List<InterviewRound> getRounds(String driveId) {
        return roundRepository.findByDriveIdOrderBySequenceAsc(driveId);
    }

    public InterviewRound getRound(String roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + roundId));
    }

    public void deleteRound(String roundId) {
        roundRepository.deleteById(roundId);
    }
}
