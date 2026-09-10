package com.smartcampus.dto;

import com.smartcampus.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    // Student-specific optional fields (used when role == STUDENT)
    private String rollNumber;
    private String department;
    private String degree;
    private Integer graduationYear;
}
