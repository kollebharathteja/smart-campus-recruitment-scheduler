package com.smartcampus.service;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Interview;
import com.smartcampus.model.Student;
import com.smartcampus.model.enums.InterviewStatus;
import com.smartcampus.model.enums.NotificationType;
import com.smartcampus.repository.InterviewRepository;
import com.smartcampus.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;

    public List<Interview> getAll() {
        return interviewRepository.findAll();
    }

    public Interview getById(String id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + id));
    }

    public List<Interview> getForStudent(String studentId) {
        return interviewRepository.findByStudentId(studentId);
    }

    public List<Interview> getForInterviewer(String interviewerId) {
        return interviewRepository.findByInterviewerIdsContaining(interviewerId);
    }

    public List<Interview> getForDrive(String driveId) {
        return interviewRepository.findByDriveId(driveId);
    }

    public Interview updateStatus(String id, InterviewStatus status) {
        Interview interview = getById(id);
        interview.setStatus(status);
        interview.setUpdatedAt(LocalDateTime.now());
        Interview saved = interviewRepository.save(interview);

        if (status == InterviewStatus.CANCELLED) {
            Student student = studentRepository.findById(interview.getStudentId()).orElse(null);
            if (student != null) {
                notificationService.notify(student.getUserId(), NotificationType.INTERVIEW_CANCELLED,
                        "Interview Cancelled", "Your interview scheduled on " + interview.getDate() + " has been cancelled.");
            }
        }
        return saved;
    }

    public Interview updateDetails(String id, String venue, String meetingLink) {
        Interview interview = getById(id);
        interview.setVenue(venue);
        interview.setMeetingLink(meetingLink);
        interview.setUpdatedAt(LocalDateTime.now());
        return interviewRepository.save(interview);
    }
}
