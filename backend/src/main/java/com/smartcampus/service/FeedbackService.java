package com.smartcampus.service;

import com.smartcampus.dto.FeedbackRequest;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.*;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.model.enums.FeedbackDecision;
import com.smartcampus.model.enums.InterviewStatus;
import com.smartcampus.model.enums.NotificationType;
import com.smartcampus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records interviewer feedback and drives multi-round progression:
 * NEXT_ROUND advances the candidate's currentRoundId, SELECTED/REJECTED/ON_HOLD
 * finalize (or pause) their status for the drive.
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRoundRepository roundRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;

    public Feedback submit(String interviewId, String interviewerId, FeedbackRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        double overall = average(request.getTechnicalScore(), request.getCommunicationScore(),
                request.getProblemSolvingScore(), request.getCodingScore());

        Feedback feedback = Feedback.builder()
                .interviewId(interviewId)
                .studentId(interview.getStudentId())
                .interviewerId(interviewerId)
                .technicalScore(request.getTechnicalScore())
                .communicationScore(request.getCommunicationScore())
                .problemSolvingScore(request.getProblemSolvingScore())
                .codingScore(request.getCodingScore())
                .overallScore(overall)
                .comments(request.getComments())
                .decision(request.getDecision())
                .createdAt(LocalDateTime.now())
                .build();

        Feedback saved = feedbackRepository.save(feedback);

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setUpdatedAt(LocalDateTime.now());
        interviewRepository.save(interview);

        applyDecision(interview, request.getDecision());

        return saved;
    }

    private double average(Integer... scores) {
        double sum = 0; int count = 0;
        for (Integer s : scores) {
            if (s != null) { sum += s; count++; }
        }
        return count == 0 ? 0 : Math.round((sum / count) * 100.0) / 100.0;
    }

    private void applyDecision(Interview interview, FeedbackDecision decision) {
        Application application = applicationRepository.findByStudentIdAndDriveId(interview.getStudentId(), interview.getDriveId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found for this interview."));

        Student student = studentRepository.findById(interview.getStudentId()).orElse(null);
        String userId = student != null ? student.getUserId() : null;

        switch (decision) {
            case SELECTED -> {
                application.setStatus(ApplicationStatus.SELECTED);
                if (student != null) {
                    student.setPlacementStatus(ApplicationStatus.SELECTED);
                    studentRepository.save(student);
                }
                notificationService.notify(userId, NotificationType.SELECTED, "Congratulations!",
                        "You have been selected. Final placement confirmation will follow.");
            }
            case REJECTED -> {
                application.setStatus(ApplicationStatus.REJECTED);
                notificationService.notify(userId, NotificationType.REJECTED, "Interview Result",
                        "You were not selected to proceed further in this drive.");
            }
            case ON_HOLD -> application.setStatus(ApplicationStatus.ON_HOLD);
            case NEXT_ROUND -> {
                List<InterviewRound> rounds = roundRepository.findByDriveIdOrderBySequenceAsc(interview.getDriveId());
                int currentIndex = -1;
                for (int i = 0; i < rounds.size(); i++) {
                    if (rounds.get(i).getId().equals(interview.getRoundId())) { currentIndex = i; break; }
                }
                if (currentIndex >= 0 && currentIndex + 1 < rounds.size()) {
                    application.setCurrentRoundId(rounds.get(currentIndex + 1).getId());
                    application.setStatus(ApplicationStatus.SHORTLISTED); // ready to be scheduled for the next round
                    notificationService.notify(userId, NotificationType.NEXT_ROUND, "Moved to Next Round",
                            "You have progressed to the next interview round: " + rounds.get(currentIndex + 1).getRoundName());
                } else {
                    // no further rounds configured -> treat as selected
                    application.setStatus(ApplicationStatus.SELECTED);
                    if (student != null) {
                        student.setPlacementStatus(ApplicationStatus.SELECTED);
                        studentRepository.save(student);
                    }
                    notificationService.notify(userId, NotificationType.SELECTED, "Congratulations!",
                            "You have cleared the final round and been selected.");
                }
            }
        }

        application.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(application);
    }

    public List<Feedback> getForInterview(String interviewId) {
        return feedbackRepository.findByInterviewId(interviewId);
    }

    public List<Feedback> getForStudent(String studentId) {
        return feedbackRepository.findByStudentId(studentId);
    }
}
