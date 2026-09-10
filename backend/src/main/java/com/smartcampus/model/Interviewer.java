package com.smartcampus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "interviewers")
public class Interviewer {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String department;

    private String designation;

    private String phone;
}
