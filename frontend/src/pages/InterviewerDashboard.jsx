import React, { useEffect, useState } from 'react';
import DashboardCard from '../components/DashboardCard.jsx';
import { interviewerApi, interviewApi, panelApi } from '../services/api';

export default function InterviewerDashboard() {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    (async () => {
      const { data: me } = await interviewerApi.getMe();
      const { data: interviews } = await interviewApi.getByInterviewer(me.id);
      const { data: panels } = await panelApi.getByInterviewer(me.id);
      const today = new Date().toISOString().slice(0, 10);
      setStats({
        today: interviews.filter((i) => i.date === today).length,
        upcoming: interviews.filter((i) => i.status === 'SCHEDULED' || i.status === 'CONFIRMED').length,
        panels: panels.length,
        total: interviews.length,
        completed: interviews.filter((i) => i.status === 'COMPLETED').length
      });
    })();
  }, []);

  if (!stats) return <p className="muted">Loading dashboard…</p>;

  return (
    <div>
      <h1>Interviewer Dashboard</h1>
      <p className="muted" style={{ marginBottom: 24 }}>Your assigned panels and interview workload.</p>
      <div className="card-grid">
        <DashboardCard label="Today's Interviews" value={stats.today} />
        <DashboardCard label="Upcoming Interviews" value={stats.upcoming} />
        <DashboardCard label="Assigned Panels" value={stats.panels} />
        <DashboardCard label="Total Candidates" value={stats.total} />
        <DashboardCard label="Completed Interviews" value={stats.completed} />
      </div>
    </div>
  );
}
