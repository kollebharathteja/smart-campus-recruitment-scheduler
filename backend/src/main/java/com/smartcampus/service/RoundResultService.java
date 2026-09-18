package com.smartcampus.service;

import com.smartcampus.dto.DriveResultDto;
import com.smartcampus.dto.RoundCandidateDto;
import com.smartcampus.dto.RoundMarksUploadRequest;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.UnauthorizedActionException;
import com.smartcampus.model.Application;
import com.smartcampus.model.Company;
import com.smartcampus.model.InterviewRound;
import com.smartcampus.model.RecruitmentDrive;
import com.smartcampus.model.RoundResult;
import com.smartcampus.model.Student;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.model.enums.NotificationType;
import com.smartcampus.model.enums.Role;
import com.smartcampus.repository.ApplicationRepository;
import com.smartcampus.repository.CompanyRepository;
import com.smartcampus.repository.InterviewRoundRepository;
import com.smartcampus.repository.InterviewerRepository;
import com.smartcampus.repository.RecruitmentDriveRepository;
import com.smartcampus.repository.RoundResultRepository;
import com.smartcampus.repository.StudentRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.AuthenticatedUser;
import com.smartcampus.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Round-by-round progression engine.
 *
 * A shortlisted student stands in exactly one round at a time (Application.currentRoundId).
 * The admin or a lecturer uploads that round's marks; anyone at or above the round's cutoff
 * moves to the next round, anyone below is rejected, and clearing the last round marks the
 * student SELECTED (and PLACED on their student record). Every outcome is emailed to the
 * student together with the schedule of whatever comes next.
 *
 * Lecturers only ever see and grade students of their own department — the scoping happens
 * here rather than in the controller so no endpoint can accidentally leak another department.
 */
@Service
@RequiredArgsConstructor
public class RoundResultService {

    private final RoundResultRepository roundResultRepository;
    private final InterviewRoundRepository roundRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final RecruitmentDriveRepository driveRepository;
    private final CompanyRepository companyRepository;
    private final InterviewerRepository interviewerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ScheduleFormatter scheduleFormatter;
    private final CurrentUserProvider currentUserProvider;

    // ---- Reads ----

    /** Students currently standing in this round, with their marks if already uploaded. */
    public List<RoundCandidateDto> getCandidates(String roundId) {
        InterviewRound round = getRound(roundId);
        Map<String, RoundResult> resultsByStudent = new HashMap<>();
        for (RoundResult r : roundResultRepository.findByRoundId(roundId)) {
            resultsByStudent.put(r.getStudentId(), r);
        }

        List<RoundCandidateDto> candidates = new ArrayList<>();
        for (Application app : applicationRepository.findByDriveId(round.getDriveId())) {
            boolean standsInRound = roundId.equals(app.getCurrentRoundId())
                    && (app.getStatus() == ApplicationStatus.SHORTLISTED || app.getStatus() == ApplicationStatus.IN_PROCESS);
            RoundResult existing = resultsByStudent.get(app.getStudentId());
            if (!standsInRound && existing == null) continue;

            Student student = studentRepository.findById(app.getStudentId()).orElse(null);
            if (student == null) continue;
            if (!visibleToCurrentUser(student.getDepartment())) continue;

            candidates.add(RoundCandidateDto.builder()
                    .studentId(student.getId())
                    .applicationId(app.getId())
                    .name(student.getName())
                    .rollNumber(student.getRollNumber())
                    .email(student.getEmail())
                    .department(student.getDepartment())
                    .applicationStatus(app.getStatus())
                    .marks(existing != null ? existing.getMarks() : null)
                    .maxMarks(existing != null ? existing.getMaxMarks() : round.getMaxMarks())
                    .cutoffMarks(existing != null ? existing.getCutoffMarks() : round.getCutoffMarks())
                    .qualified(existing != null ? existing.isQualified() : null)
                    .remarks(existing != null ? existing.getRemarks() : null)
                    .build());
        }

        candidates.sort(Comparator.comparing(RoundCandidateDto::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return candidates;
    }

    public List<RoundResult> getResults(String roundId) {
        return roundResultRepository.findByRoundId(roundId).stream()
                .filter(r -> visibleToCurrentUser(r.getDepartment()))
                .sorted(Comparator.comparing(RoundResult::getStudentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    /** Per-student outcome for one drive: every round's marks plus whether they were selected. */
    public List<DriveResultDto> getDriveResults(String driveId) {
        RecruitmentDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruitment drive not found: " + driveId));
        return buildDriveResults(drive, applicationRepository.findByDriveId(driveId));
    }

    /**
     * Every drive's outcome for the students the caller is allowed to see — a lecturer gets
     * only their own department, an admin gets everyone.
     */
    public List<DriveResultDto> getAllResults() {
        List<DriveResultDto> all = new ArrayList<>();
        for (RecruitmentDrive drive : driveRepository.findAll()) {
            all.addAll(buildDriveResults(drive, applicationRepository.findByDriveId(drive.getId())));
        }
        return all;
    }

    private List<DriveResultDto> buildDriveResults(RecruitmentDrive drive, List<Application> applications) {
        String companyName = companyRepository.findById(drive.getCompanyId()).map(Company::getName)
                .orElse("Unknown company");
        List<InterviewRound> rounds = roundRepository.findByDriveIdOrderBySequenceAsc(drive.getId());

        Map<String, Map<String, RoundResult>> resultsByStudent = new HashMap<>();
        for (RoundResult r : roundResultRepository.findByDriveId(drive.getId())) {
            resultsByStudent.computeIfAbsent(r.getStudentId(), k -> new HashMap<>()).put(r.getRoundId(), r);
        }

        List<DriveResultDto> results = new ArrayList<>();
        for (Application app : applications) {
            Student student = studentRepository.findById(app.getStudentId()).orElse(null);
            if (student == null) continue;
            if (!visibleToCurrentUser(student.getDepartment())) continue;

            Map<String, RoundResult> studentResults = resultsByStudent.getOrDefault(student.getId(), Map.of());
            List<DriveResultDto.RoundMarkDto> roundMarks = rounds.stream()
                    .map(round -> {
                        RoundResult rr = studentResults.get(round.getId());
                        return DriveResultDto.RoundMarkDto.builder()
                                .roundId(round.getId())
                                .roundName(round.getRoundName())
                                .sequence(round.getSequence())
                                .marks(rr != null ? rr.getMarks() : null)
                                .maxMarks(rr != null ? rr.getMaxMarks() : round.getMaxMarks())
                                .cutoffMarks(rr != null ? rr.getCutoffMarks() : round.getCutoffMarks())
                                .qualified(rr != null ? rr.isQualified() : null)
                                .remarks(rr != null ? rr.getRemarks() : null)
                                .build();
                    })
                    .toList();

            String currentRoundName = rounds.stream()
                    .filter(r -> r.getId().equals(app.getCurrentRoundId()))
                    .findFirst().map(InterviewRound::getRoundName).orElse(null);

            results.add(DriveResultDto.builder()
                    .studentId(student.getId())
                    .applicationId(app.getId())
                    .studentName(student.getName())
                    .rollNumber(student.getRollNumber())
                    .email(student.getEmail())
                    .department(student.getDepartment())
                    .driveId(drive.getId())
                    .companyName(companyName)
                    .jobRole(drive.getJobRole())
                    .status(app.getStatus())
                    .currentRoundName(currentRoundName)
                    .rounds(roundMarks)
                    .build());
        }
        return results;
    }

    // ---- Marks upload + progression ----

    public List<RoundResult> uploadMarks(String roundId, RoundMarksUploadRequest request) {
        InterviewRound round = getRound(roundId);
        RecruitmentDrive drive = driveRepository.findById(round.getDriveId()).orElse(null);
        List<InterviewRound> rounds = roundRepository.findByDriveIdOrderBySequenceAsc(round.getDriveId());
        InterviewRound nextRound = nextRound(rounds, round);

        if (request.getCutoffMarks() != null) round.setCutoffMarks(request.getCutoffMarks());
        if (request.getMaxMarks() != null) round.setMaxMarks(request.getMaxMarks());
        roundRepository.save(round);

        User uploader = userRepository.findById(currentUserProvider.getCurrentUserId()).orElse(null);
        List<RoundResult> saved = new ArrayList<>();

        for (RoundMarksUploadRequest.Entry entry : request.getEntries() == null ? List.<RoundMarksUploadRequest.Entry>of() : request.getEntries()) {
            Student student = resolveStudent(entry);
            if (student == null) continue;
            if (!visibleToCurrentUser(student.getDepartment())) {
                throw new UnauthorizedActionException(
                        "You can only upload marks for students of your own department.");
            }

            Application application = applicationRepository
                    .findByStudentIdAndDriveId(student.getId(), round.getDriveId())
                    .orElse(null);
            if (application == null) continue;

            boolean qualified = entry.getQualified() != null
                    ? entry.getQualified()
                    : entry.getMarks() != null && round.getCutoffMarks() != null
                        && entry.getMarks() >= round.getCutoffMarks();

            RoundResult result = roundResultRepository.findByRoundIdAndStudentId(roundId, student.getId())
                    .orElseGet(RoundResult::new);
            result.setDriveId(round.getDriveId());
            result.setRoundId(roundId);
            result.setStudentId(student.getId());
            result.setApplicationId(application.getId());
            result.setStudentName(student.getName());
            result.setRollNumber(student.getRollNumber());
            result.setEmail(student.getEmail());
            result.setDepartment(student.getDepartment());
            result.setRoundName(round.getRoundName());
            result.setSequence(round.getSequence());
            result.setMarks(entry.getMarks());
            result.setMaxMarks(round.getMaxMarks());
            result.setCutoffMarks(round.getCutoffMarks());
            result.setQualified(qualified);
            result.setRemarks(entry.getRemarks());
            result.setUploadedByUserId(uploader != null ? uploader.getId() : null);
            result.setUploadedByName(uploader != null ? uploader.getName() : null);
            result.setUploadedAt(LocalDateTime.now());
            saved.add(roundResultRepository.save(result));

            advanceApplication(application, student, drive, round, nextRound, result);
        }

        return saved;
    }

    /** Moves the application forward/out based on this round's result, then emails the student. */
    private void advanceApplication(Application application, Student student, RecruitmentDrive drive,
                                    InterviewRound round, InterviewRound nextRound, RoundResult result) {
        String companyName = drive != null
                ? companyRepository.findById(drive.getCompanyId()).map(Company::getName).orElse("the company")
                : "the company";
        String jobRole = drive != null ? drive.getJobRole() : "";
        String roundLabel = scheduleFormatter.roundLabel(round);
        String scoreLine = result.getMarks() == null
                ? ""
                : String.format("Your score: %s%s%s%n", trimNumber(result.getMarks()),
                        result.getMaxMarks() != null ? " / " + trimNumber(result.getMaxMarks()) : "",
                        result.getCutoffMarks() != null ? " (cut-off " + trimNumber(result.getCutoffMarks()) + ")" : "");

        if (!result.isQualified()) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setUpdatedAt(LocalDateTime.now());
            applicationRepository.save(application);

            String body = "Hi " + student.getName() + ",\n\n"
                    + "Thank you for appearing for " + roundLabel + " of the " + companyName
                    + " drive for the role of " + jobRole + ".\n"
                    + scoreLine
                    + "\nYou have not been shortlisted for the next round this time. Keep an eye on the portal — "
                    + "more drives are on the way.\n\nAll the best,\nT&P Office";
            notificationService.notifyAndEmail(student.getUserId(), student.getEmail(), NotificationType.REJECTED,
                    roundLabel + " result",
                    "You did not clear " + roundLabel + " for " + companyName + ".",
                    companyName + " — " + roundLabel + " result", body);
            return;
        }

        if (nextRound != null) {
            application.setStatus(ApplicationStatus.IN_PROCESS);
            application.setCurrentRoundId(nextRound.getId());
            application.setUpdatedAt(LocalDateTime.now());
            applicationRepository.save(application);

            String body = "Hi " + student.getName() + ",\n\n"
                    + "Congratulations! You have qualified " + roundLabel + " of the " + companyName
                    + " drive for the role of " + jobRole + ".\n"
                    + scoreLine
                    + "\nNext up: " + scheduleFormatter.roundLabel(nextRound) + "\n\n"
                    + scheduleFormatter.scheduleBlock(student.getId(), drive, nextRound)
                    + "\nPlease log in to the Smart Campus Recruitment portal to track your rounds and marks.\n\n"
                    + "All the best,\nT&P Office";
            notificationService.notifyAndEmail(student.getUserId(), student.getEmail(), NotificationType.NEXT_ROUND,
                    "Qualified " + roundLabel,
                    "You qualified " + roundLabel + " for " + companyName + ". Next: "
                            + scheduleFormatter.roundLabel(nextRound) + ".",
                    companyName + " — you qualified " + roundLabel, body);
            return;
        }

        application.setStatus(ApplicationStatus.SELECTED);
        application.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(application);

        student.setPlacementStatus(ApplicationStatus.SELECTED);
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.save(student);

        String body = "Hi " + student.getName() + ",\n\n"
                + "Congratulations! You have cleared the final round (" + roundLabel + ") and have been SELECTED by "
                + companyName + " for the role of " + jobRole + ".\n"
                + scoreLine
                + "\nYour selection is now recorded on the Smart Campus Recruitment portal. The T&P office will "
                + "share the offer formalities with you shortly.\n\nCongratulations once again,\nT&P Office";
        notificationService.notifyAndEmail(student.getUserId(), student.getEmail(), NotificationType.SELECTED,
                "You have been selected!",
                "You cleared every round and have been selected by " + companyName + ".",
                "Selected: " + companyName + " — " + jobRole, body);
    }

    // ---- Helpers ----

    private InterviewRound getRound(String roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + roundId));
    }

    private InterviewRound nextRound(List<InterviewRound> rounds, InterviewRound current) {
        for (int i = 0; i < rounds.size(); i++) {
            if (rounds.get(i).getId().equals(current.getId())) {
                return i + 1 < rounds.size() ? rounds.get(i + 1) : null;
            }
        }
        return null;
    }

    private Student resolveStudent(RoundMarksUploadRequest.Entry entry) {
        if (entry.getStudentId() != null && !entry.getStudentId().isBlank()) {
            return studentRepository.findById(entry.getStudentId()).orElse(null);
        }
        if (entry.getRollNumber() != null && !entry.getRollNumber().isBlank()) {
            return studentRepository.findByRollNumber(entry.getRollNumber().trim()).orElse(null);
        }
        return null;
    }

    /** Admins see every department; a lecturer only ever sees their own. */
    private boolean visibleToCurrentUser(String department) {
        Optional<String> scope = lecturerDepartment();
        if (scope.isEmpty()) return true;
        return department != null && department.trim().equalsIgnoreCase(scope.get().trim());
    }

    private Optional<String> lecturerDepartment() {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        if (!Role.INTERVIEWER.name().equals(user.role())) {
            return Optional.empty();
        }
        return interviewerRepository.findByUserId(user.userId())
                .map(i -> i.getDepartment())
                .filter(d -> d != null && !d.isBlank());
    }

    private String trimNumber(Double value) {
        if (value == null) return "";
        return value == Math.floor(value) ? String.valueOf(value.longValue()) : String.valueOf(value);
    }
}
