package com.smartcampus.service;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Interview;
import com.smartcampus.model.InterviewRound;
import com.smartcampus.model.RecruitmentDrive;
import com.smartcampus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final RecruitmentDriveRepository driveRepository;
    private final InterviewRoundRepository roundRepository;
    private final InterviewPanelRepository panelRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final FeedbackRepository feedbackRepository;

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
        deleteDriveCascade(id);
    }

    /** Deletes a drive and everything hanging off it: rounds, panels, applications, interviews, feedback. */
    public void deleteDriveCascade(String driveId) {
        List<Interview> interviews = interviewRepository.findByDriveId(driveId);
        for (Interview interview : interviews) {
            feedbackRepository.deleteAll(feedbackRepository.findByInterviewId(interview.getId()));
        }
        interviewRepository.deleteAll(interviews);
        applicationRepository.deleteAll(applicationRepository.findByDriveId(driveId));
        panelRepository.deleteAll(panelRepository.findByDriveId(driveId));
        roundRepository.deleteAll(roundRepository.findByDriveIdOrderBySequenceAsc(driveId));
        driveRepository.deleteById(driveId);
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
