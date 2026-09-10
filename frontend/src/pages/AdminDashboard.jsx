import React, { useEffect, useState } from 'react';
import DashboardCard from '../components/DashboardCard.jsx';
import { studentApi, companyApi, driveApi, interviewApi, reportApi } from '../services/api';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    (async () => {
      const [students, companies, drives, interviews, placements] = await Promise.all([
        studentApi.getAll(), companyApi.getAll(), driveApi.getAll(), interviewApi.getAll(), reportApi.placements()
      ]);
      const today = new Date().toISOString().slice(0, 10);
      setStats({
        totalStudents: students.data.length,
        totalCompanies: companies.data.length,
        activeDrives: drives.data.filter((d) => d.status === 'OPEN').length,
        scheduledInterviews: interviews.data.filter((i) => i.status === 'SCHEDULED').length,
        todaysInterviews: interviews.data.filter((i) => i.date === today).length,
        completedInterviews: interviews.data.filter((i) => i.status === 'COMPLETED').length,
        selected: placements.data.totalPlaced,
        totalStudentsPlacement: placements.data.totalStudents
      });
    })();
  }, []);

  if (!stats) return <p className="muted">Loading dashboard…</p>;

  return (
    <div>
      <h1>T&amp;P Dashboard</h1>
      <p className="muted" style={{ marginBottom: 24 }}>Live overview of the campus recruitment pipeline.</p>
      <div className="card-grid">
        <DashboardCard label="Total Students" value={stats.totalStudents} />
        <DashboardCard label="Total Companies" value={stats.totalCompanies} />
        <DashboardCard label="Active Drives" value={stats.activeDrives} />
        <DashboardCard label="Scheduled Interviews" value={stats.scheduledInterviews} />
        <DashboardCard label="Today's Interviews" value={stats.todaysInterviews} />
        <DashboardCard label="Completed Interviews" value={stats.completedInterviews} />
        <DashboardCard label="Selected Students" value={stats.selected} />
        <DashboardCard label="Not Yet Placed" value={stats.totalStudentsPlacement - stats.selected} />
      </div>
      <div className="card">
        <h3>Getting started</h3>
        <p className="muted">
          1. Add a Company → 2. Create a Recruitment Drive with dynamic requirements → 3. Review eligible students →
          4. Shortlist candidates → 5. Create interview panels & rounds → 6. Collect availability →
          7. Run the automated clash-free Scheduler → 8. Track interviews on the Calendar → 9. Review Reports.
        </p>
      </div>
    </div>
  );
}
