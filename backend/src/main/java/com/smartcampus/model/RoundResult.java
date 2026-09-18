package com.smartcampus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Marks a student scored in one round of one drive, as uploaded by the admin or a lecturer.
 * One document per student per round; re-uploading marks for the same student overwrites it.
 * The student's name/roll/department/email are denormalized so a results sheet stays readable
 * even if the student record is later edited.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roundResults")
@CompoundIndexes({
        @CompoundIndex(name = "round_student_unique", def = "{'roundId': 1, 'studentId': 1}", unique = true)
})
public class RoundResult {

    @Id
    private String id;

    @Indexed
    private String driveId;

    @Indexed
    private String roundId;

    @Indexed
    private String studentId;

    private String applicationId;

    private String studentName;
    private String rollNumber;
    private String email;

    @Indexed
    private String department;

    private String roundName;
    private Integer sequence;

    private Double marks;
    private Double maxMarks;
    private Double cutoffMarks;

    private boolean qualified;

    private String remarks;

    private String uploadedByUserId;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
