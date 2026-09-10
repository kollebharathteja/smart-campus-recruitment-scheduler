package com.smartcampus.model;

import com.smartcampus.model.enums.ApplicationStatus;
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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "applications")
@CompoundIndexes({
        @CompoundIndex(name = "student_drive_unique", def = "{'studentId': 1, 'driveId': 1}", unique = true)
})
public class Application {

    @Id
    private String id;

    @Indexed
    private String studentId;

    @Indexed
    private String driveId;

    @Indexed
    private ApplicationStatus status;

    /** List of reasons if NOT_ELIGIBLE, populated by EligibilityService. */
    private List<String> eligibilityReasons;

    /** id of the InterviewRound the candidate currently stands in. */
    private String currentRoundId;

    private LocalDateTime appliedAt;
    private LocalDateTime shortlistedAt;
    private LocalDateTime updatedAt;
}
