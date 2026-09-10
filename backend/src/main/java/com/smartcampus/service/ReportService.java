package com.smartcampus.service;

import com.smartcampus.model.Application;
import com.smartcampus.model.RecruitmentDrive;
import com.smartcampus.model.Student;
import com.smartcampus.model.enums.ApplicationStatus;
import com.smartcampus.model.enums.InterviewStatus;
import com.smartcampus.repository.ApplicationRepository;
import com.smartcampus.repository.InterviewRepository;
import com.smartcampus.repository.RecruitmentDriveRepository;
import com.smartcampus.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final RecruitmentDriveRepository driveRepository;
    private final InterviewRepository interviewRepository;

    public Map<String, Object> placementSummary() {
        List<Student> students = studentRepository.findAll();
        long placed = students.stream().filter(s -> s.getPlacementStatus() == ApplicationStatus.SELECTED).count();

        List<Application> applications = applicationRepository.findAll();
        Map<ApplicationStatus, Long> byStatus = applications.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        Map<InterviewStatus, Long> interviewsByStatus = interviewRepository.findAll().stream()
                .collect(Collectors.groupingBy(com.smartcampus.model.Interview::getStatus, Collectors.counting()));

        return Map.of(
                "totalStudents", students.size(),
                "totalPlaced", placed,
                "totalNotPlaced", students.size() - placed,
                "applicationsByStatus", byStatus,
                "interviewsByStatus", interviewsByStatus
        );
    }

    public Map<String, Object> companyWise() {
        List<RecruitmentDrive> drives = driveRepository.findAll();
        return drives.stream().collect(Collectors.toMap(
                RecruitmentDrive::getId,
                drive -> {
                    long selected = applicationRepository.findByDriveId(drive.getId()).stream()
                            .filter(a -> a.getStatus() == ApplicationStatus.SELECTED).count();
                    long total = applicationRepository.findByDriveId(drive.getId()).size();
                    return Map.of("jobRole", drive.getJobRole(), "totalApplications", total, "selected", selected);
                }
        ));
    }

    public Map<String, Object> departmentWise() {
        List<Student> students = studentRepository.findAll();
        return students.stream()
                .collect(Collectors.groupingBy(Student::getDepartment, Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> Map.of(
                                "totalStudents", list.size(),
                                "placed", list.stream().filter(s -> s.getPlacementStatus() == ApplicationStatus.SELECTED).count()
                        )
                )));
    }
}
