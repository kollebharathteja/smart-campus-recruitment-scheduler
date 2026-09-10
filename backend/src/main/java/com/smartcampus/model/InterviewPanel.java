package com.smartcampus.model;

import com.smartcampus.model.enums.PanelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "interviewPanels")
public class InterviewPanel {

    @Id
    private String id;

    @Indexed
    private String driveId;

    @Indexed
    private String roundId;

    private String panelName;

    /** List of Interviewer._id (ObjectId references). */
    private List<String> interviewerIds;

    private String venue;

    private PanelStatus status;
}
