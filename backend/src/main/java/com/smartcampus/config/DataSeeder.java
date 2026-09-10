package com.smartcampus.config;

import com.smartcampus.model.*;
import com.smartcampus.model.enums.*;
import com.smartcampus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds a minimal, realistic dataset on first boot (only if the "users" collection is empty)
 * so the scheduler/eligibility engine can be demonstrated immediately.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InterviewerRepository interviewerRepository;
    private final CompanyRepository companyRepository;
    private final RecruitmentDriveRepository driveRepository;
    private final InterviewRoundRepository roundRepository;
    private final InterviewPanelRepository panelRepository;
    private final ApplicationRepository applicationRepository;
    private final AvailabilityRepository availabilityRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded
        }

        // ---- Admin ----
        User admin = userRepository.save(User.builder()
                .name("T&P Officer")
                .email("admin@campus.edu")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build());

        // ---- Interviewers ----
        User interviewerUser1 = userRepository.save(User.builder()
                .name("Dr. Ananya Rao").email("ananya.rao@campus.edu")
                .password(passwordEncoder.encode("interviewer123"))
                .role(Role.INTERVIEWER).enabled(true).createdAt(LocalDateTime.now()).build());
        Interviewer interviewer1 = interviewerRepository.save(Interviewer.builder()
                .userId(interviewerUser1.getId()).name("Dr. Ananya Rao").email("ananya.rao@campus.edu")
                .department("Computer Science").designation("Associate Professor").phone("9000000001").build());

        User interviewerUser2 = userRepository.save(User.builder()
                .name("Mr. Karthik Iyer").email("karthik.iyer@campus.edu")
                .password(passwordEncoder.encode("interviewer123"))
                .role(Role.INTERVIEWER).enabled(true).createdAt(LocalDateTime.now()).build());
        Interviewer interviewer2 = interviewerRepository.save(Interviewer.builder()
                .userId(interviewerUser2.getId()).name("Mr. Karthik Iyer").email("karthik.iyer@campus.edu")
                .department("Information Technology").designation("Assistant Professor").phone("9000000002").build());

        // ---- Students ----
        User studentUser1 = userRepository.save(User.builder()
                .name("Rahul Sharma").email("rahul.sharma@campus.edu")
                .password(passwordEncoder.encode("student123"))
                .role(Role.STUDENT).enabled(true).createdAt(LocalDateTime.now()).build());
        Student student1 = studentRepository.save(Student.builder()
                .userId(studentUser1.getId()).name("Rahul Sharma").email("rahul.sharma@campus.edu")
                .rollNumber("MCA2027001").department("Computer Applications").degree("MCA")
                .cgpa(8.2).backlogs(0).graduationYear(2027)
                .skills(List.of("Java", "Spring Boot", "MongoDB"))
                .placementStatus(ApplicationStatus.APPLIED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        User studentUser2 = userRepository.save(User.builder()
                .name("Priya Verma").email("priya.verma@campus.edu")
                .password(passwordEncoder.encode("student123"))
                .role(Role.STUDENT).enabled(true).createdAt(LocalDateTime.now()).build());
        Student student2 = studentRepository.save(Student.builder()
                .userId(studentUser2.getId()).name("Priya Verma").email("priya.verma@campus.edu")
                .rollNumber("MCA2027002").department("Computer Applications").degree("MCA")
                .cgpa(6.8).backlogs(1).graduationYear(2027)
                .skills(List.of("Java", "MongoDB"))
                .placementStatus(ApplicationStatus.APPLIED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        User studentUser3 = userRepository.save(User.builder()
                .name("Aditya Kumar").email("aditya.kumar@campus.edu")
                .password(passwordEncoder.encode("student123"))
                .role(Role.STUDENT).enabled(true).createdAt(LocalDateTime.now()).build());
        Student student3 = studentRepository.save(Student.builder()
                .userId(studentUser3.getId()).name("Aditya Kumar").email("aditya.kumar@campus.edu")
                .rollNumber("MCA2027003").department("Computer Applications").degree("MCA")
                .cgpa(9.1).backlogs(0).graduationYear(2027)
                .skills(List.of("Java", "Spring Boot", "MongoDB", "React"))
                .placementStatus(ApplicationStatus.APPLIED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        // ---- Company & Drive ----
        Company company = companyRepository.save(Company.builder()
                .name("ABC Technologies")
                .description("Enterprise software solutions provider")
                .website("https://abctechnologies.example.com")
                .hrContactName("Neha Gupta")
                .hrContactEmail("hr@abctech.example.com")
                .createdAt(LocalDateTime.now()).build());

        RecruitmentDrive drive = driveRepository.save(RecruitmentDrive.builder()
                .companyId(company.getId())
                .jobRole("Java Developer")
                .description("Backend development role using Java, Spring Boot and MongoDB.")
                .requirements(RecruitmentRequirements.builder()
                        .minimumCgpa(7.0)
                        .maximumBacklogs(0)
                        .eligibleDegrees(List.of("MCA", "B.Tech"))
                        .eligibleDepartments(List.of())
                        .graduationYear(2027)
                        .requiredSkills(List.of("Java", "Spring Boot", "MongoDB"))
                        .build())
                .applicationDeadline(LocalDate.now().plusDays(10))
                .driveDate(LocalDate.now().plusDays(20))
                .status(DriveStatus.OPEN)
                .createdAt(LocalDateTime.now()).build());

        // ---- Rounds ----
        InterviewRound round1 = roundRepository.save(InterviewRound.builder()
                .driveId(drive.getId()).roundName("Technical Interview").sequence(1).durationMinutes(30).build());
        roundRepository.save(InterviewRound.builder()
                .driveId(drive.getId()).roundName("HR Interview").sequence(2).durationMinutes(20).build());

        // ---- Panel ----
        panelRepository.save(InterviewPanel.builder()
                .driveId(drive.getId()).roundId(round1.getId()).panelName("Panel 1")
                .interviewerIds(List.of(interviewer1.getId(), interviewer2.getId()))
                .venue("Seminar Hall").status(PanelStatus.ACTIVE).build());

        // ---- Applications (student1 & student3 eligible, student2 not) ----
        applicationRepository.save(Application.builder()
                .studentId(student1.getId()).driveId(drive.getId())
                .status(ApplicationStatus.SHORTLISTED)
                .currentRoundId(round1.getId())
                .appliedAt(LocalDateTime.now()).shortlistedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build());

        applicationRepository.save(Application.builder()
                .studentId(student3.getId()).driveId(drive.getId())
                .status(ApplicationStatus.SHORTLISTED)
                .currentRoundId(round1.getId())
                .appliedAt(LocalDateTime.now()).shortlistedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build());

        applicationRepository.save(Application.builder()
                .studentId(student2.getId()).driveId(drive.getId())
                .status(ApplicationStatus.NOT_ELIGIBLE)
                .eligibilityReasons(List.of(
                        "CGPA is 6.8; minimum required is 7.0",
                        "Maximum allowed backlogs is 0, student has 1"))
                .appliedAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        // ---- Sample availability for the demo (student1, interviewer1 & interviewer2) ----
        LocalDate interviewDate = LocalDate.now().plusDays(7);

        availabilityRepository.save(Availability.builder()
                .ownerId(student1.getId()).ownerType("STUDENT").date(interviewDate)
                .slots(List.of(TimeSlot.builder().startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build()))
                .build());

        availabilityRepository.save(Availability.builder()
                .ownerId(student3.getId()).ownerType("STUDENT").date(interviewDate)
                .slots(List.of(TimeSlot.builder().startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(13, 0)).build()))
                .build());

        availabilityRepository.save(Availability.builder()
                .ownerId(interviewer1.getId()).ownerType("INTERVIEWER").date(interviewDate)
                .slots(List.of(TimeSlot.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(13, 0)).build()))
                .build());

        availabilityRepository.save(Availability.builder()
                .ownerId(interviewer2.getId()).ownerType("INTERVIEWER").date(interviewDate)
                .slots(List.of(TimeSlot.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(13, 0)).build()))
                .build());

        System.out.println("=========================================================");
        System.out.println(" Smart Campus Recruitment - sample data seeded.");
        System.out.println(" Admin login:       admin@campus.edu / admin123");
        System.out.println(" Student login:     rahul.sharma@campus.edu / student123");
        System.out.println(" Interviewer login: ananya.rao@campus.edu / interviewer123");
        System.out.println("=========================================================");
    }
}
