package com.smartcampus.service;

import com.smartcampus.dto.ApplicationSummaryDto;
import com.smartcampus.dto.EligibilityResultDto;
import com.smartcampus.exception.DuplicateApplicationException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.*;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.model.enums.NotificationType;
import com.smartcampus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Handles the "Student Applies -> Admin Shortlists" part of the workflow.
 * Delegates the actual pass/fail decision to EligibilityService (single source of truth).
 *
 * Shortlisting a student (individually or via "Shortlist All Eligible") is also the moment
 * a student's login account gets auto-provisioned if they don't already have one, and the
 * moment they get emailed with the company/round details. A student who qualifies for
 * several companies simply accumulates one Application per drive, each shortlist triggering
 * its own email — their single login shows all of them.
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final InterviewRoundRepository roundRepository;
    private final RecruitmentDriveRepository driveRepository;
    private final CompanyRepository companyRepository;
    private final InterviewPanelRepository panelRepository;
    private final InterviewRepository interviewRepository;
    private final FeedbackRepository feedbackRepository;
    private final EligibilityService eligibilityService;
    private final NotificationService notificationService;
    private final StudentService studentService;
    private final RoundResultRepository roundResultRepository;
    private final ScheduleFormatter scheduleFormatter;

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
        return doShortlist(application);
    }

    /**
     * Finds (or creates) the Application for this student+drive and shortlists it. This is
     * what "Shortlist All Eligible" drives per student: unlike shortlist(applicationId), the
     * student doesn't need to have applied first — being eligible is enough, since a T&P
     * office bulk-imports students who may never have self-applied to anything.
     */
    public Application shortlistStudentForDrive(String studentId, String driveId) {
        Application application = applicationRepository.findByStudentIdAndDriveId(studentId, driveId)
                .orElseGet(() -> applicationRepository.save(Application.builder()
                        .studentId(studentId)
                        .driveId(driveId)
                        .status(ApplicationStatus.APPLIED)
                        .eligibilityReasons(List.of())
                        .appliedAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        // Already progressed past "shortlisted" (or already shortlisted) — don't re-notify/re-email.
        if (application.getStatus() == ApplicationStatus.SHORTLISTED
                || application.getStatus() == ApplicationStatus.IN_PROCESS
                || application.getStatus() == ApplicationStatus.SELECTED) {
            return application;
        }

        return doShortlist(application);
    }

    /** Shared shortlist logic: assign first round, provision login if needed, notify + email. */
    private Application doShortlist(Application application) {
        List<InterviewRound> rounds = roundRepository.findByDriveIdOrderBySequenceAsc(application.getDriveId());
        String firstRoundId = rounds.isEmpty() ? null : rounds.get(0).getId();

        application.setStatus(ApplicationStatus.SHORTLISTED);
        application.setCurrentRoundId(firstRoundId);
        application.setShortlistedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        Application saved = applicationRepository.save(application);

        Student student = studentRepository.findById(application.getStudentId()).orElse(null);
        if (student == null) {
            return saved;
        }

        RecruitmentDrive drive = driveRepository.findById(application.getDriveId()).orElse(null);
        String companyName = drive != null
                ? companyRepository.findById(drive.getCompanyId()).map(Company::getName).orElse("the company")
                : "the company";
        String jobRole = drive != null ? drive.getJobRole() : "";
        InterviewRound firstRound = firstRoundId == null ? null : roundRepository.findById(firstRoundId).orElse(null);
        String roundLabel = firstRound != null ? scheduleFormatter.roundLabel(firstRound) : "the first round";

        // Auto-provision a login the first time this student is shortlisted for anything.
        // If they already have one (self-registered, or shortlisted for an earlier company),
        // it's reused as-is and only the email content changes.
        StudentService.LoginProvisionResult login;
        try {
            login = studentService.ensureLoginAccount(student);
        } catch (IllegalStateException ex) {
            // Missing roll number/email — shortlisting still succeeds, just without a login/email.
            login = null;
        }

        String inAppMessage = String.format(
                "You've been shortlisted by %s for %s — %s. Please submit your interview availability.",
                companyName, jobRole, roundLabel);

        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(student.getName()).append(",\n\n");
        body.append("Congratulations! You have been shortlisted by ").append(companyName)
                .append(" for the role of ").append(jobRole).append(".\n\n");
        body.append("Next up: ").append(roundLabel).append("\n\n");
        body.append(scheduleFormatter.scheduleBlock(student.getId(), drive, firstRound));
        body.append("\nPlease log in to the Smart Campus Recruitment portal to submit your interview ")
                .append("availability and track every round's status and marks.\n");
        body.append("Login email: ").append(student.getEmail()).append("\n");
        if (login != null && login.newlyCreated()) {
            body.append("Your login has just been created for you. Password: ").append(login.rawPassword())
                    .append(" (this is your roll/hall-ticket number — please change it after logging in.)\n");
        } else {
            body.append("Use your existing portal login to view full details.\n");
        }
        body.append("\nAll the best,\nT&P Office");

        notificationService.notifyAndEmail(student.getUserId(), NotificationType.SHORTLISTED,
                "You've been shortlisted!", inAppMessage,
                "You're shortlisted: " + companyName + " — " + jobRole, body.toString());

        return saved;
    }

    /**
     * Bulk-shortlists every student the eligibility engine currently marks as eligible for
     * this drive — regardless of whether they'd already applied. This is what the admin's
     * "Shortlist All Eligible" button triggers: each shortlisted student gets a login
     * (auto-created if they didn't have one) and an email with the company/round details.
     * A student eligible for multiple drives/companies gets one email + one Application per
     * drive; their single login shows all of them.
     */
    public List<Application> shortlistAllEligible(String driveId) {
        List<EligibilityResultDto> eligible = eligibilityService.getEligibleStudents(driveId);
        List<Application> results = new ArrayList<>();
        for (EligibilityResultDto r : eligible) {
            results.add(shortlistStudentForDrive(r.getStudentId(), driveId));
        }
        return results;
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

    /**
     * Everything a student needs to see about their own applications — company, role, round,
     * status, and marks/feedback per round completed so far — in one call, for the student
     * dashboard.
     */
    public List<ApplicationSummaryDto> getSummaryForStudent(String studentId) {
        List<Application> applications = applicationRepository.findByStudentId(studentId);
        List<ApplicationSummaryDto> summaries = new ArrayList<>();

        for (Application app : applications) {
            RecruitmentDrive drive = driveRepository.findById(app.getDriveId()).orElse(null);
            String companyName = drive != null
                    ? companyRepository.findById(drive.getCompanyId()).map(Company::getName).orElse("Unknown company")
                    : "Unknown company";
            String jobRole = drive != null ? drive.getJobRole() : "";

            List<InterviewRound> rounds = drive != null
                    ? roundRepository.findByDriveIdOrderBySequenceAsc(drive.getId())
                    : List.of();

            InterviewRound currentRound = app.getCurrentRoundId() == null ? null
                    : rounds.stream().filter(r -> r.getId().equals(app.getCurrentRoundId())).findFirst().orElse(null);

            List<Interview> interviews = drive != null
                    ? interviewRepository.findByStudentId(studentId).stream()
                        .filter(i -> i.getDriveId() != null && i.getDriveId().equals(drive.getId()))
                        .toList()
                    : List.of();
            Map<String, Interview> interviewByRound = new java.util.HashMap<>();
            for (Interview iv : interviews) {
                if (iv.getRoundId() != null) interviewByRound.put(iv.getRoundId(), iv);
            }

            List<ApplicationSummaryDto.RoundResultDto> roundResults = rounds.stream()
                    .sorted(Comparator.comparing(InterviewRound::getSequence))
                    .map(round -> {
                        Interview interview = interviewByRound.get(round.getId());
                        String panelName = panelRepository.findByRoundId(round.getId()).stream()
                                .findFirst().map(InterviewPanel::getPanelName).orElse(null);

                        RoundResult roundResult = roundResultRepository
                                .findByRoundIdAndStudentId(round.getId(), studentId).orElse(null);

                        Double overallScore = null;
                        String decision = null;
                        String comments = null;
                        if (interview != null) {
                            List<Feedback> feedbacks = feedbackRepository.findByInterviewId(interview.getId());
                            if (!feedbacks.isEmpty()) {
                                Feedback f = feedbacks.get(0);
                                overallScore = f.getOverallScore();
                                decision = f.getDecision() != null ? f.getDecision().name() : null;
                                comments = f.getComments();
                            }
                        }

                        return ApplicationSummaryDto.RoundResultDto.builder()
                                .roundId(round.getId())
                                .roundName(round.getRoundName())
                                .sequence(round.getSequence())
                                .panelName(panelName)
                                .interviewStatus(interview != null && interview.getStatus() != null
                                        ? interview.getStatus().name() : null)
                                .interviewDate(interview != null ? interview.getDate() : null)
                                .venue(interview != null ? interview.getVenue() : null)
                                .meetingLink(interview != null ? interview.getMeetingLink() : null)
                                .overallScore(overallScore)
                                .decision(decision)
                                .comments(comments)
                                .marks(roundResult != null ? roundResult.getMarks() : null)
                                .maxMarks(roundResult != null ? roundResult.getMaxMarks() : round.getMaxMarks())
                                .cutoffMarks(roundResult != null ? roundResult.getCutoffMarks() : round.getCutoffMarks())
                                .qualified(roundResult != null ? roundResult.isQualified() : null)
                                .build();
                    })
                    .toList();

            summaries.add(ApplicationSummaryDto.builder()
                    .applicationId(app.getId())
                    .driveId(app.getDriveId())
                    .companyName(companyName)
                    .jobRole(jobRole)
                    .status(app.getStatus())
                    .currentRoundName(currentRound != null ? currentRound.getRoundName() : null)
                    .currentRoundSequence(currentRound != null ? currentRound.getSequence() : null)
                    .shortlistedAt(app.getShortlistedAt())
                    .eligibilityReasons(app.getEligibilityReasons())
                    .roundResults(roundResults)
                    .build());
        }

        return summaries;
    }
}
