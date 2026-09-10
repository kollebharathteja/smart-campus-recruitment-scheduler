package com.smartcampus.model;

import com.smartcampus.model.enums.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "interviews")
@CompoundIndexes({
        @CompoundIndex(name = "date_status_idx", def = "{'date': 1, 'status': 1}"),
        @CompoundIndex(name = "panel_date_idx", def = "{'panelId': 1, 'date': 1}")
})
public class Interview {

    @Id
    private String id;

    @Indexed
    private String driveId;

    @Indexed
    private String studentId;

    @Indexed
    private String panelId;

    @Indexed
    private List<String> interviewerIds;

    @Indexed
    private String roundId;

    @Indexed
    private LocalDate date;

    private LocalTime startTime;
    private LocalTime endTime;

    @Indexed
    private InterviewStatus status;

    private String venue;
    private String meetingLink;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
