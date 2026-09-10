package com.smartcampus.service;

import com.smartcampus.dto.ScheduleRequest;
import com.smartcampus.dto.ScheduleResultDto;
import com.smartcampus.dto.UnscheduledCandidateDto;
import com.smartcampus.exception.NoAvailableSlotException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.SchedulingConflictException;
import com.smartcampus.model.*;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.model.enums.InterviewStatus;
import com.smartcampus.model.enums.NotificationType;
import com.smartcampus.model.enums.SchedulingStrategy;
import com.smartcampus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Core differentiator #2: the automated CLASH-FREE interview scheduler.
 *
 * For every shortlisted candidate in a round, the scheduler:
 *   1. Loads the candidate's declared availability.
 *   2. Loads the assigned panel's interviewers' declared availability.
 *   3. Intersects both windows to compute the set of common minutes.
 *   4. Slices the intersection into duration-sized slots.
 *   5. Discards any slot that clashes with an existing interview for the
 *      student, any interviewer on the panel, or the panel itself.
 *   6. Assigns the first slot that survives all the conflict checks
 *      (or applies the configured SchedulingStrategy), across the
 *      candidate's allowed dates in order.
 *   7. Candidates for whom no valid slot exists on any candidate date are
 *      reported back as UNSCHEDULED with a precise reason, rather than
 *      silently failing.
 *
 * Two intervals [aStart, aEnd) and [bStart, bEnd) overlap iff:
 *      aStart < bEnd AND aEnd > bStart
 * This exact rule is used throughout for all conflict checks.
 */
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final InterviewerRepository interviewerRepository;
    private final InterviewPanelRepository panelRepository;
    private final InterviewRoundRepository roundRepository;
    private final AvailabilityRepository availabilityRepository;
    private final InterviewRepository interviewRepository;
    private final NotificationService notificationService;

    private static final String STUDENT = "STUDENT";
    private static final String INTERVIEWER = "INTERVIEWER";

    public ScheduleResultDto generateSchedule(ScheduleRequest request) {
        InterviewRound round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + request.getRoundId()));

        List<InterviewPanel> panels = panelRepository.findByRoundId(request.getRoundId());
        if (panels.isEmpty()) {
            throw new SchedulingConflictException("No interview panel has been assigned to this round yet.");
        }
        // For simplicity/demo determinism we use the first active panel found for the round;
        // BALANCED_PANEL_DISTRIBUTION strategy rotates across all panels for the round.
        int panelIndex = 0;

        int durationMinutes = request.getDurationMinutes() != null ? request.getDurationMinutes()
                : (round.getDurationMinutes() != null ? round.getDurationMinutes() : 30);

        List<LocalDate> candidateDates = (request.getCandidateDates() == null || request.getCandidateDates().isEmpty())
                ? List.of(LocalDate.now().plusDays(7))
                : request.getCandidateDates();

        SchedulingStrategy strategy = request.getStrategy() == null ? SchedulingStrategy.FIRST_AVAILABLE : request.getStrategy();

        List<Application> candidates = resolveCandidates(request);

        List<String> scheduledIds = new ArrayList<>();
        List<UnscheduledCandidateDto> unscheduled = new ArrayList<>();

        for (Application application : candidates) {
            InterviewPanel panel = panels.get(panelIndex % panels.size());
            if (strategy == SchedulingStrategy.BALANCED_PANEL_DISTRIBUTION) {
                panelIndex++;
            }

            Student student = studentRepository.findById(application.getStudentId()).orElse(null);
            if (student == null) {
                unscheduled.add(UnscheduledCandidateDto.builder()
                        .studentId(application.getStudentId())
                        .studentName("Unknown")
                        .reason("Student record could not be found.")
                        .build());
                continue;
            }

            Optional<Interview> result = scheduleOneCandidate(student, application, panel, round, durationMinutes, candidateDates, strategy);

            if (result.isPresent()) {
                scheduledIds.add(result.get().getId());
                application.setStatus(ApplicationStatus.IN_PROCESS);
                application.setUpdatedAt(LocalDateTime.now());
                applicationRepository.save(application);

                notificationService.notify(student.getUserId(), NotificationType.INTERVIEW_SCHEDULED,
                        "Interview Scheduled",
                        String.format("Round: %s | Date: %s | Time: %s - %s | Panel: %s | Venue: %s",
                                round.getRoundName(), result.get().getDate(), result.get().getStartTime(),
                                result.get().getEndTime(), panel.getPanelName(), panel.getVenue()));
            } else {
                unscheduled.add(UnscheduledCandidateDto.builder()
                        .studentId(student.getId())
                        .studentName(student.getName())
                        .reason("No common availability found between student and assigned interview panel on the candidate dates.")
                        .build());
            }
        }

        return ScheduleResultDto.builder()
                .scheduledInterviewIds(scheduledIds)
                .unscheduled(unscheduled)
                .build();
    }

    private List<Application> resolveCandidates(ScheduleRequest request) {
        if (request.getApplicationIds() != null && !request.getApplicationIds().isEmpty()) {
            return applicationRepository.findAllById(request.getApplicationIds());
        }
        return applicationRepository.findByDriveId(request.getDriveId()).stream()
                .filter(a -> a.getStatus() == ApplicationStatus.SHORTLISTED || a.getStatus() == ApplicationStatus.IN_PROCESS)
                .filter(a -> request.getRoundId().equals(a.getCurrentRoundId()))
                .toList();
    }

    private Optional<Interview> scheduleOneCandidate(Student student, Application application, InterviewPanel panel,
                                                       InterviewRound round, int durationMinutes,
                                                       List<LocalDate> candidateDates, SchedulingStrategy strategy) {

        for (LocalDate date : candidateDates) {
            List<TimeSlot> studentWindows = getSlots(student.getId(), STUDENT, date);
            if (studentWindows.isEmpty()) continue;

            List<TimeSlot> panelWindows = intersectPanelAvailability(panel.getInterviewerIds(), date);
            if (panelWindows.isEmpty()) continue;

            List<TimeSlot> commonWindows = intersectAll(studentWindows, panelWindows);
            if (commonWindows.isEmpty()) continue;

            List<TimeSlot> candidateSlots = slice(commonWindows, durationMinutes);
            if (strategy == SchedulingStrategy.STUDENT_PRIORITY || strategy == SchedulingStrategy.EARLIEST_AVAILABLE) {
                candidateSlots.sort(Comparator.comparing(TimeSlot::getStartTime));
            }

            for (TimeSlot slot : candidateSlots) {
                if (hasConflict(student.getId(), panel, date, slot)) {
                    continue; // try next slot - automatically finds an alternative
                }

                Interview interview = Interview.builder()
                        .driveId(application.getDriveId())
                        .studentId(student.getId())
                        .panelId(panel.getId())
                        .interviewerIds(panel.getInterviewerIds())
                        .roundId(round.getId())
                        .date(date)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .status(InterviewStatus.SCHEDULED)
                        .venue(panel.getVenue())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                return Optional.of(interviewRepository.save(interview));
            }
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------
    // Availability retrieval & interval math
    // ---------------------------------------------------------------

    private List<TimeSlot> getSlots(String ownerId, String ownerType, LocalDate date) {
        return availabilityRepository.findByOwnerIdAndOwnerTypeAndDate(ownerId, ownerType, date)
                .map(Availability::getSlots)
                .orElse(List.of());
    }

    /** Intersection of every interviewer's availability on the panel for the given date. */
    private List<TimeSlot> intersectPanelAvailability(List<String> interviewerIds, LocalDate date) {
        List<TimeSlot> running = null;
        for (String interviewerId : interviewerIds) {
            List<TimeSlot> windows = getSlots(interviewerId, INTERVIEWER, date);
            if (windows.isEmpty()) {
                return List.of(); // one interviewer unavailable -> whole panel unavailable that date
            }
            running = (running == null) ? windows : intersectAll(running, windows);
            if (running.isEmpty()) return List.of();
        }
        return running == null ? List.of() : running;
    }

    /** Pairwise intersection between two sets of windows, producing the overlapping sub-windows. */
    private List<TimeSlot> intersectAll(List<TimeSlot> a, List<TimeSlot> b) {
        List<TimeSlot> result = new ArrayList<>();
        for (TimeSlot windowA : a) {
            for (TimeSlot windowB : b) {
                LocalTime start = windowA.getStartTime().isAfter(windowB.getStartTime()) ? windowA.getStartTime() : windowB.getStartTime();
                LocalTime end = windowA.getEndTime().isBefore(windowB.getEndTime()) ? windowA.getEndTime() : windowB.getEndTime();
                if (start.isBefore(end)) {
                    result.add(TimeSlot.builder().startTime(start).endTime(end).build());
                }
            }
        }
        return result;
    }

    /** Slice a set of windows into fixed-duration candidate slots, in chronological order. */
    private List<TimeSlot> slice(List<TimeSlot> windows, int durationMinutes) {
        List<TimeSlot> slots = new ArrayList<>();
        for (TimeSlot window : windows) {
            LocalTime cursor = window.getStartTime();
            while (!cursor.plusMinutes(durationMinutes).isAfter(window.getEndTime())) {
                LocalTime end = cursor.plusMinutes(durationMinutes);
                slots.add(TimeSlot.builder().startTime(cursor).endTime(end).build());
                cursor = end;
            }
        }
        slots.sort(Comparator.comparing(TimeSlot::getStartTime));
        return slots;
    }

    /**
     * Checks student / interviewer / panel double-booking using the interval-overlap rule:
     *   existingStart < newEnd AND existingEnd > newStart
     */
    private boolean hasConflict(String studentId, InterviewPanel panel, LocalDate date, TimeSlot candidate) {
        // Student conflict - across ANY drive/panel on this date
        for (Interview existing : interviewRepository.findByStudentIdAndDate(studentId, date)) {
            if (isActive(existing) && overlaps(existing, candidate)) return true;
        }

        // Panel conflict - this panel already has an interview at an overlapping time
        for (Interview existing : interviewRepository.findByPanelIdAndDate(panel.getId(), date)) {
            if (isActive(existing) && overlaps(existing, candidate)) return true;
        }

        // Interviewer conflict - any interviewer on this panel already booked elsewhere (different panel too)
        for (String interviewerId : panel.getInterviewerIds()) {
            for (Interview existing : interviewRepository.findByInterviewerIdsContainingAndDate(interviewerId, date)) {
                if (isActive(existing) && overlaps(existing, candidate)) return true;
            }
        }

        return false;
    }

    private boolean isActive(Interview interview) {
        return interview.getStatus() != InterviewStatus.CANCELLED;
    }

    private boolean overlaps(Interview existing, TimeSlot candidate) {
        return existing.getStartTime().isBefore(candidate.getEndTime())
                && existing.getEndTime().isAfter(candidate.getStartTime());
    }

    // ---------------------------------------------------------------
    // Reschedule / Find Alternative Slot
    // ---------------------------------------------------------------

    public Interview reschedule(String interviewId, List<LocalDate> candidateDates) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        InterviewPanel panel = panelRepository.findById(interview.getPanelId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview panel not found: " + interview.getPanelId()));
        InterviewRound round = roundRepository.findById(interview.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + interview.getRoundId()));
        Student student = studentRepository.findById(interview.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + interview.getStudentId()));

        int durationMinutes = (int) java.time.Duration.between(interview.getStartTime(), interview.getEndTime()).toMinutes();

        // Temporarily mark old slot cancelled so it doesn't self-conflict during search
        InterviewStatus previousStatus = interview.getStatus();
        interview.setStatus(InterviewStatus.CANCELLED);
        interviewRepository.save(interview);

        Application application = applicationRepository.findByStudentIdAndDriveId(student.getId(), interview.getDriveId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found for this interview."));

        Optional<Interview> newInterview = scheduleOneCandidate(student, application, panel, round, durationMinutes, candidateDates, SchedulingStrategy.FIRST_AVAILABLE);

        if (newInterview.isEmpty()) {
            // restore old interview - could not find an alternative
            interview.setStatus(previousStatus);
            interviewRepository.save(interview);
            throw new NoAvailableSlotException("No alternative clash-free slot could be found for this candidate.");
        }

        interviewRepository.deleteById(interview.getId());
        Interview finalInterview = newInterview.get();
        finalInterview.setStatus(InterviewStatus.RESCHEDULED);
        finalInterview.setUpdatedAt(LocalDateTime.now());
        Interview saved = interviewRepository.save(finalInterview);

        notificationService.notify(student.getUserId(), NotificationType.INTERVIEW_RESCHEDULED,
                "Interview Rescheduled",
                String.format("Your interview has been moved to %s, %s - %s.", saved.getDate(), saved.getStartTime(), saved.getEndTime()));

        return saved;
    }

    public List<Interview> getUnscheduledPlaceholder() {
        // Unscheduled candidates are not persisted as Interview docs (there's nothing to schedule yet);
        // callers should rely on the `unscheduled` list returned by generateSchedule(). This method
        // exists to support GET /api/scheduler/unscheduled returning the most recent result if cached.
        return List.of();
    }
}
