package com.smartcampus.dto;

import lombok.Data;

import java.util.List;

/**
 * Marks sheet for one round, uploaded by an admin or a lecturer.
 * cutoffMarks/maxMarks are stored back on the round so later uploads (and the results view)
 * reuse the same pass mark. A per-student {@code qualified} flag overrides the cutoff when a
 * round is judged rather than scored (e.g. an HR round marked pass/fail by hand).
 */
@Data
public class RoundMarksUploadRequest {

    private Double cutoffMarks;
    private Double maxMarks;
    private List<Entry> entries;

    @Data
    public static class Entry {
        private String studentId;
        private String rollNumber;
        private Double marks;
        private Boolean qualified;
        private String remarks;
    }
}
