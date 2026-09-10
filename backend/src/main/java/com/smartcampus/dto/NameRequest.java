package com.smartcampus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NameRequest {
    @NotBlank
    private String name;
}
