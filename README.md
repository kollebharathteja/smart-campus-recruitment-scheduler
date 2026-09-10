# Smart Campus Recruitment & Interview Scheduler

An MCA major project: a college-specific Training & Placement (T&P) management
system that automates the full campus recruitment workflow — from dynamic
eligibility checking through **automated clash-free interview scheduling** to
final placement reporting.

```
Student Management → Company Requirements → Dynamic Eligibility Checking →
Automatic Shortlisting → Interview Panels → Availability →
Automated Clash-Free Scheduling → Calendar → Interview → Feedback →
Next Round → Final Selection → Reports
```

## Tech stack

| Layer     | Technology |
|-----------|------------|
| Frontend  | React.js, React Router, Axios, FullCalendar, Recharts, Vite |
| Backend   | Java 17, Spring Boot 3.3, Spring Data MongoDB, Spring Security, JWT |
| Database  | MongoDB (no SQL/JPA/Hibernate anywhere) |

## Project structure

```
project/
├── backend/     Spring Boot REST API (Maven project)
└── frontend/    React single-page app (Vite)
```

## The two core differentiators

1. **`EligibilityService`** (`backend/.../service/EligibilityService.java`) —
   evaluates every student against a drive's own dynamic
   `RecruitmentRequirements` document (CGPA, backlogs, degree, department,
   graduation year, required skills) and returns a precise, human-readable
   reason for every failed check. Nothing is hard-coded per company.

2. **`SchedulerService`** (`backend/.../service/SchedulerService.java`) — the
   automated clash-free interview scheduler. For each shortlisted candidate it
   intersects student availability with the assigned panel's interviewer
   availability, slices the overlap into duration-sized slots, and rejects any
   slot that clashes with an existing interview for the student, any
   interviewer on the panel, or the panel itself, using the interval-overlap
   rule `existingStart < newEnd AND existingEnd > newStart`. Candidates who
   cannot be scheduled are reported back with a specific reason instead of
   silently failing.

## Prerequisites

- Java 17+
- Maven 3.9+ (or use the included `mvnw` if you add one)
- Node.js 18+ and npm
- MongoDB running locally on `mongodb://localhost:27017` (or update
  `backend/src/main/resources/application.yml`)

## Running the backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. On first boot, `DataSeeder` seeds
a small demo dataset (only if the `users` collection is empty) including:

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@campus.edu | admin123 |
| Student (eligible) | rahul.sharma@campus.edu | student123 |
| Student (ineligible) | priya.verma@campus.edu | student123 |
| Student (eligible) | aditya.kumar@campus.edu | student123 |
| Interviewer | ananya.rao@campus.edu | interviewer123 |
| Interviewer | karthik.iyer@campus.edu | interviewer123 |

This seeds one company (ABC Technologies), one open drive (Java Developer,
min CGPA 7.0, 0 backlogs, skills Java/Spring Boot/MongoDB), two interview
rounds, one panel, three applications (two eligible & shortlisted, one
rejected for CGPA/backlogs), and sample availability seven days out — enough
to demonstrate `POST /api/scheduler/generate` immediately.

## Running the frontend

```bash
cd frontend
npm install
npm run dev
```

The app starts on `http://localhost:3000` and proxies `/api/**` requests to
`http://localhost:8080` (see `vite.config.js`).

## Demonstrating the scheduler

1. Log in as Admin.
2. Go to **Recruitment Drives → Manage** on the seeded "Java Developer" drive,
   check the **Eligibility** tab (Rahul & Aditya eligible, Priya rejected with
   reasons), then **Applications** tab to confirm both eligible candidates are
   already `SHORTLISTED`.
3. Go to **Scheduler**, pick the drive, the "Technical Interview" round, a
   30-minute duration, and the date that was seeded (today + 7 days — check
   the sample data console log on backend startup for the exact date), then
   click **Generate Clash-Free Schedule**.
4. Open **Interview Calendar** to see the newly booked, non-overlapping
   interview slots.
5. Log in as an interviewer, open **Interview Feedback**, submit feedback and
   a decision (Next Round / Selected / Rejected) for a completed interview —
   watch the candidate's application status and student placement status
   update automatically.
6. Log in as Admin and check **Reports** for live placement analytics.

## REST API summary

See `backend/src/main/java/com/smartcampus/controller` for the full list.
Key endpoints:

```
POST /api/auth/register | /api/auth/login
GET  /api/recruitments/{id}/eligible-students | /ineligible-students
POST /api/applications                          (apply)
POST /api/applications/{id}/shortlist
POST /api/scheduler/generate                     (core scheduler)
POST /api/scheduler/reschedule                   (find alternative slot)
POST /api/interviews/{id}/feedback
GET  /api/reports/placements | /company-wise | /department-wise
```

## MongoDB collections

`users`, `students`, `companies`, `recruitmentDrives`, `applications`,
`interviewers`, `interviewPanels`, `availability`, `interviewRounds`,
`interviews`, `feedback`, `notifications` — see each `@Document` class under
`backend/src/main/java/com/smartcampus/model` for the exact document shape,
and the `@Indexed` / `@CompoundIndexes` annotations for the indexing strategy
described in the project brief (student CGPA/backlogs/degree/skills,
application student/drive/status, interview student/interviewer/panel/date,
drive company/status).

## Notes for the presentation

- The eligibility engine and scheduler are deliberately isolated into their
  own services (`EligibilityService`, `SchedulerService`) so they can be
  discussed and demonstrated independently of the CRUD scaffolding, as
  requested in the project brief.
- Interview rounds are fully dynamic per drive (see `InterviewRound` model);
  the seeded drive uses two rounds, but the Admin UI (`Recruitment Drives →
  Manage → Rounds` tab) lets you add or remove rounds for any drive without
  code changes.
- This build was generated without internet access to Maven Central, so the
  backend has not been compiled in this environment — review
  `backend/pom.xml` and run `mvn -q compile` after downloading dependencies
  to catch any environment-specific issues before your demo.
