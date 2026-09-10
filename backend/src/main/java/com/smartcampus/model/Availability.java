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

import java.time.LocalDate;
import java.util.List;

/**
 * A single document stores one person's availability for one date.
 * ownerType distinguishes STUDENT vs INTERVIEWER availability while reusing one collection,
 * as required by the "availability" collection design.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "availability")
@CompoundIndexes({
        @CompoundIndex(name = "owner_date_idx", def = "{'ownerId': 1, 'ownerType': 1, 'date': 1}")
})
public class Availability {

    @Id
    private String id;

    @Indexed
    private String ownerId; // studentId or interviewerId

    private String ownerType; // "STUDENT" or "INTERVIEWER"

    @Indexed
    private LocalDate date;

    private List<TimeSlot> slots;
}
