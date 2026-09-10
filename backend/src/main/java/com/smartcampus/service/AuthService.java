package com.smartcampus.service;

import com.smartcampus.dto.JwtResponse;
import com.smartcampus.dto.LoginRequest;
import com.smartcampus.dto.RegisterRequest;
import com.smartcampus.exception.PendingApprovalException;
import com.smartcampus.exception.UnauthorizedActionException;
import com.smartcampus.model.Interviewer;
import com.smartcampus.model.Student;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.model.enums.Role;
import com.smartcampus.repository.InterviewerRepository;
import com.smartcampus.repository.StudentRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InterviewerRepository interviewerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public JwtResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be self-registered.");
        }

        boolean isLecturer = request.getRole() == Role.INTERVIEWER;

        User user = userRepository.save(User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(!isLecturer) // lecturers start disabled until an admin approves them
                .createdAt(LocalDateTime.now())
                .build());

        if (request.getRole() == Role.STUDENT) {
            studentRepository.save(Student.builder()
                    .userId(user.getId())
                    .name(request.getName())
                    .email(request.getEmail())
                    .rollNumber(request.getRollNumber())
                    .department(request.getDepartment())
                    .degree(request.getDegree())
                    .cgpa(0.0)
                    .backlogs(0)
                    .graduationYear(request.getGraduationYear())
                    .skills(java.util.List.of())
                    .placementStatus(ApplicationStatus.APPLIED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        } else if (request.getRole() == Role.INTERVIEWER) {
            interviewerRepository.save(Interviewer.builder()
                    .userId(user.getId())
                    .name(request.getName())
                    .email(request.getEmail())
                    .department(request.getDepartment())
                    .build());
        }

        if (isLecturer) {
            throw new PendingApprovalException(
                    "Your lecturer account has been created and is awaiting admin approval. " +
                    "You'll be able to sign in once an admin approves it.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new JwtResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        if (!user.isEnabled()) {
            if (user.getRole() == Role.INTERVIEWER) {
                throw new UnauthorizedActionException("Your lecturer account is still awaiting admin approval.");
            }
            throw new UnauthorizedActionException("This account has been disabled.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new JwtResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public void changePassword(String userId, com.smartcampus.dto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.smartcampus.exception.ResourceNotFoundException("Account not found."));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /** Admin-triggered reset — no current password needed, for when a user is locked out. */
    public void adminResetPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.smartcampus.exception.ResourceNotFoundException("Account not found."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
