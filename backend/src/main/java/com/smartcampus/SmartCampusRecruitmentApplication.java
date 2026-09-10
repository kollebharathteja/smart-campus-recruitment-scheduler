package com.smartcampus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Smart Campus Recruitment & Interview Scheduler
 * MCA Major Project - College-specific Training & Placement (T&P) management system.
 *
 * Core flow:
 * Student Management -> Company Recruitment Requirements -> Dynamic Eligibility Checking ->
 * Automatic Shortlisting -> Interview Panel Management -> Availability ->
 * Automated Clash-Free Interview Scheduling -> Calendar -> Interview -> Feedback ->
 * Next Round -> Final Selection -> Reports
 */
@SpringBootApplication
public class SmartCampusRecruitmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartCampusRecruitmentApplication.class, args);
    }
}
