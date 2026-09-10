package com.smartcampus.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcampus.dto.PendingLecturerDto;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Interviewer;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;
import com.smartcampus.repository.InterviewerRepository;
import com.smartcampus.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewerService {

    private final InterviewerRepository interviewerRepository;
    private final UserRepository userRepository;

    public List<Interviewer> getAll() {
        return interviewerRepository.findAll();
    }

    public Interviewer getById(String id) {
        return interviewerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found: " + id));
    }

    public Interviewer getByUserId(String userId) {
        return interviewerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer profile not found for this account."));
    }

    public Interviewer create(Interviewer interviewer) {
        return interviewerRepository.save(interviewer);
    }

    public Interviewer update(String id, Interviewer updates) {
        Interviewer existing = getById(id);
        existing.setName(updates.getName());
        existing.setDepartment(updates.getDepartment());
        existing.setDesignation(updates.getDesignation());
        existing.setPhone(updates.getPhone());
        return interviewerRepository.save(existing);
    }

    public void delete(String id) {
        interviewerRepository.deleteById(id);
    }

    // ---- Admin approval workflow ----

    public List<PendingLecturerDto> getPendingLecturers() {
        return userRepository.findByRoleAndEnabled(Role.INTERVIEWER, false).stream()
                .map(u -> {
                    String department = interviewerRepository.findByUserId(u.getId())
                            .map(Interviewer::getDepartment).orElse(null);
                    return PendingLecturerDto.builder()
                            .userId(u.getId())
                            .name(u.getName())
                            .email(u.getEmail())
                            .department(department)
                            .requestedAt(u.getCreatedAt())
                            .build();
                })
                .toList();
    }

    public void approveLecturer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + userId));
        user.setEnabled(true);
        userRepository.save(user);
    }

    public void rejectLecturer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + userId));
        interviewerRepository.findByUserId(userId).ifPresent(interviewerRepository::delete);
        userRepository.delete(user);
    }
}
