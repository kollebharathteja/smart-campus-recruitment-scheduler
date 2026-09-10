package com.smartcampus.dto;

import com.smartcampus.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String userId;
    private String name;
    private String email;
    private Role role;
}
