package com.smartcampus.service;

import com.smartcampus.dto.EligibilityResultDto;
import com.smartcampus.exception.DuplicateApplicationException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.*;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.model.enums.NotificationType;
import com.smartcampus.repository.ApplicationRepository;
import com.smartcampus.repository.CompanyRepository;
import com.smartcampus.repository.InterviewRoundRepository;
import com.smartcampus.repository.RecruitmentDriveRepository;
import com.smartcampus.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles the "Student Applies -> Admin Shortlists" part of the workflow.
 * Delegates the actual pass/fail decision to EligibilityService (single source of truth).
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final InterviewRoundRepository roundRepository;
    private final RecruitmentDriveRepository driveRepository;
    private final CompanyRepository companyRepository;
    private final EligibilityService eligibilityService;
    private final NotificationService notificationService;

    public Application apply(String studentId, String driveId) {
        if (applicationRepository.existsByStudentIdAndDriveId(studentId, driveId)) {
            throw new DuplicateApplicationException("You have already applied to this recruitment drive.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        EligibilityResultDto eligibility = eligibilityService.evaluateStudentForDrive(studentId, driveId);

        Application application = Application.builder()
                .studentId(studentId)
                .driveId(driveId)
                .status(eligibility.isEligible() ? ApplicationStatus.APPLIED : ApplicationStatus.NOT_ELIGIBLE)
                .eligibilityReasons(eligibility.getReasons())
                .appliedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Application saved = applicationRepository.save(application);

        notificationService.notify(student.getUserId(), NotificationType.APPLICATION_CONFIRMATION,
                "Application submitted",
                "Your application has been recorded. Eligibility status: " + application.getStatus());

        return saved;
    }

    public List<Application> getByDrive(String driveId) {
        return applicationRepository.findByDriveId(driveId);
    }

    public List<Application> getByStudent(String studentId) {
        return applicationRepository.findByStudentId(studentId);
    }

    public Application getById(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    /** Admin shortlists a single eligible application and assigns it to the drive's first round. */
    public Application shortlist(String applicationId) {
        Application application = getById(applicationId);
        if (application.getStatus() == ApplicationStatus.NOT_ELIGIBLE) {
            throw new IllegalArgumentException("Cannot shortlist a NOT_ELIGIBLE candidate.");
        }

        List<com.smartcampus.model.InterviewRound> rounds = roundRepository.findByDriveIdOrderBySequenceAsc(application.getDriveId());
        String firstRoundId = rounds.isEmpty() ? null : rounds.get(0).getId();

        application.setStatus(ApplicationStatus.SHORTLISTED);
        application.setCurrentRoundId(firstRoundId);
        application.setShortlistedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        Application saved = applicationRepository.save(application);

        Student student = studentRepository.findById(application.getStudentId()).orElse(null);
        if (student != null) {
            RecruitmentDrive drive = driveRepository.findById(application.getDriveId()).orElse(null);
            String companyName = drive != null
                    ? companyRepository.findById(drive.getCompanyId()).map(Company::getName).orElse("the company")
                    : "the company";
            String jobRole = drive != null ? drive.getJobRole() : "";
            InterviewRound firstRound = firstRoundId == null ? null : roundRepository.findById(firstRoundId).orElse(null);
            String roundLabel = firstRound != null
                    ? String.format("Round %d: %s", firstRound.getSequence(), firstRound.getRoundName())
                    : "the first round";

            notificationService.notify(student.getUserId(), NotificationType.SHORTLISTED,
                    "You've been shortlisted!",
                    String.format("You've been shortlisted by %s for %s — %s. Please submit your interview availability.",
                            companyName, jobRole, roundLabel));
        }

        return saved;
    }

    /** Bulk-shortlist every currently eligible (APPLIED) application for a drive. */
    public List<Application> shortlistAllEligible(String driveId) {
        List<Application> eligibleApps = applicationRepository.findByDriveIdAndStatus(driveId, ApplicationStatus.APPLIED);
        return eligibleApps.stream().map(a -> shortlist(a.getId())).toList();
    }

    public Application reject(String applicationId) {
        Application application = getById(applicationId);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setUpdatedAt(LocalDateTime.now());
        Application saved = applicationRepository.save(application);

        Student student = studentRepository.findById(application.getStudentId()).orElse(null);
        if (student != null) {
            notificationService.notify(student.getUserId(), NotificationType.REJECTED,
                    "Application update", "You were not selected to proceed further in this drive.");
        }
        return saved;
    }

    public Application updateStatus(String applicationId, ApplicationStatus status) {
        Application application = getById(applicationId);
        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }
}
