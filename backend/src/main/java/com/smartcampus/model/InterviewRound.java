package com.smartcampus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Dynamically defined interview round for a specific drive.
 * A drive can have any number/sequence of rounds (Aptitude, Coding, Technical, HR, ...).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "interviewRounds")
public class InterviewRound {

    @Id
    private String id;

    @Indexed
    private String driveId;

    private String roundName;

    private Integer sequence; // order of the round within the drive

    private Integer durationMinutes; // default interview slot duration for this round
}
