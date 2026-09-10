package com.smartcampus.controller;

import com.smartcampus.model.Student;
import com.smartcampus.security.CurrentUserProvider;
import com.smartcampus.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<Student>> getAll() {
        return ResponseEntity.ok(studentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getById(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<Student> getMyProfile() {
        return ResponseEntity.ok(studentService.getByUserId(currentUserProvider.getCurrentUserId()));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Student> updateMyProfile(@RequestBody com.smartcampus.dto.StudentProfileUpdateRequest request) {
        return ResponseEntity.ok(studentService.updateOwnProfile(currentUserProvider.getCurrentUserId(), request));
    }

    @PostMapping
    public ResponseEntity<Student> create(@RequestBody Student student) {
        return ResponseEntity.ok(studentService.create(student));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> update(@PathVariable String id, @RequestBody Student student) {
        return ResponseEntity.ok(studentService.update(id, student));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
