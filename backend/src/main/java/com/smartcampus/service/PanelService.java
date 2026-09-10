package com.smartcampus.service;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.InterviewPanel;
import com.smartcampus.model.enums.PanelStatus;
import com.smartcampus.repository.InterviewPanelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PanelService {

    private final InterviewPanelRepository panelRepository;

    public InterviewPanel create(InterviewPanel panel) {
        if (panel.getStatus() == null) {
            panel.setStatus(PanelStatus.ACTIVE);
        }
        return panelRepository.save(panel);
    }

    public List<InterviewPanel> getByDrive(String driveId) {
        return panelRepository.findByDriveId(driveId);
    }

    public List<InterviewPanel> getByRound(String roundId) {
        return panelRepository.findByRoundId(roundId);
    }

    public List<InterviewPanel> getByInterviewer(String interviewerId) {
        return panelRepository.findByInterviewerIdsContaining(interviewerId);
    }

    public InterviewPanel getById(String id) {
        return panelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview panel not found: " + id));
    }

    public InterviewPanel rename(String id, String newName) {
        InterviewPanel panel = getById(id);
        panel.setPanelName(newName);
        return panelRepository.save(panel);
    }

    public InterviewPanel addInterviewer(String id, String interviewerId) {
        InterviewPanel panel = getById(id);
        if (!panel.getInterviewerIds().contains(interviewerId)) {
            panel.getInterviewerIds().add(interviewerId);
        }
        return panelRepository.save(panel);
    }

    public InterviewPanel removeInterviewer(String id, String interviewerId) {
        InterviewPanel panel = getById(id);
        panel.getInterviewerIds().remove(interviewerId);
        return panelRepository.save(panel);
    }

    public InterviewPanel update(String id, InterviewPanel updates) {
        InterviewPanel panel = getById(id);
        panel.setPanelName(updates.getPanelName());
        panel.setInterviewerIds(updates.getInterviewerIds());
        panel.setVenue(updates.getVenue());
        panel.setStatus(updates.getStatus());
        panel.setRoundId(updates.getRoundId());
        return panelRepository.save(panel);
    }

    public void delete(String id) {
        panelRepository.deleteById(id);
    }
}
