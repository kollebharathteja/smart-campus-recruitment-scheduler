package com.smartcampus.service;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Student;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;
import com.smartcampus.repository.StudentRepository;
import com.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    public Student getById(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

    public Student getByUserId(String userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for this account."));
    }

    public Student create(Student student) {
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        return studentRepository.save(student);
    }

    public Student update(String id, Student updates) {
        Student existing = getById(id);
        existing.setName(updates.getName());
        existing.setDepartment(updates.getDepartment());
        existing.setDegree(updates.getDegree());
        existing.setCgpa(updates.getCgpa());
        existing.setBacklogs(updates.getBacklogs());
        existing.setGraduationYear(updates.getGraduationYear());
        existing.setSkills(updates.getSkills());
        existing.setResumeUrl(updates.getResumeUrl());
        existing.setPhone(updates.getPhone());
        existing.setAdditionalDetails(updates.getAdditionalDetails());
        existing.setUpdatedAt(LocalDateTime.now());
        return studentRepository.save(existing);
    }

    public void delete(String id) {
        studentRepository.deleteById(id);
    }

    /**
     * Result of {@link #ensureLoginAccount}. rawPassword is only populated when a brand-new
     * account was created (so it can be emailed once) — it is never stored or re-returned
     * for an account that already existed.
     */
    public record LoginProvisionResult(User user, boolean newlyCreated, String rawPassword) {}

    /**
     * Makes sure a Student has a working login account, auto-creating one when they don't.
     * Called whenever a student becomes shortlisted for a drive — students who were only ever
     * imported via Excel/added by the T&amp;P office (and never self-registered) get a real
     * login the first time they qualify for something.
     *
     * Username = the student's email. Password = the student's roll/hall-ticket number
     * (BCrypt-hashed), so it's something the student already knows and the office can quote
     * verbally if needed. If a login already exists (self-registered earlier, or provisioned
     * for a previous company), it is left untouched and simply reused/linked.
     */
    public LoginProvisionResult ensureLoginAccount(Student student) {
        if (student.getUserId() != null) {
            Optional<User> existing = userRepository.findById(student.getUserId());
            if (existing.isPresent()) {
                return new LoginProvisionResult(existing.get(), false, null);
            }
        }

        Optional<User> byEmail = userRepository.findByEmail(student.getEmail());
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (student.getUserId() == null) {
                student.setUserId(user.getId());
                student.setUpdatedAt(LocalDateTime.now());
                studentRepository.save(student);
            }
            return new LoginProvisionResult(user, false, null);
        }

        if (student.getRollNumber() == null || student.getRollNumber().isBlank()) {
            throw new IllegalStateException(
                    "Cannot auto-create a login for " + student.getName() + " — no roll/hall-ticket number on file.");
        }
        if (student.getEmail() == null || student.getEmail().isBlank()) {
            throw new IllegalStateException(
                    "Cannot auto-create a login for " + student.getName() + " — no email on file.");
        }

        String rawPassword = student.getRollNumber().trim();
        User user = User.builder()
                .name(student.getName())
                .email(student.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.STUDENT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        student.setUserId(user.getId());
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.save(student);

        return new LoginProvisionResult(user, true, rawPassword);
    }

    /** Students may only touch their own contact info, skills and self-reported academic history here. */
    public Student updateOwnProfile(String userId, com.smartcampus.dto.StudentProfileUpdateRequest req) {
        Student existing = getByUserId(userId);
        existing.setPhone(req.getPhone());
        existing.setResumeUrl(req.getResumeUrl());
        if (req.getSkills() != null) {
            existing.setSkills(req.getSkills());
        }
        if (req.getAdditionalDetails() != null) {
            existing.setAdditionalDetails(req.getAdditionalDetails());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return studentRepository.save(existing);
    }
}
