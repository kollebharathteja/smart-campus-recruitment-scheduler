package com.smartcampus.service;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Student;
import com.smartcampus.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

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

    /** Students may only touch their own contact info — never the eligibility-relevant fields. */
    public Student updateOwnProfile(String userId, com.smartcampus.dto.StudentProfileUpdateRequest req) {
        Student existing = getByUserId(userId);
        existing.setPhone(req.getPhone());
        existing.setResumeUrl(req.getResumeUrl());
        existing.setUpdatedAt(LocalDateTime.now());
        return studentRepository.save(existing);
    }
}
