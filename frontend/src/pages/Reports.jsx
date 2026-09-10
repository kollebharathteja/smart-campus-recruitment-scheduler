import React, { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';
import { reportApi } from '../services/api';
import DashboardCard from '../components/DashboardCard.jsx';

const COLORS = ['#263a68', '#d68c33', '#2f7d5f', '#b3402f', '#5a6274', '#8a6fb0'];

export default function Reports() {
  const [placements, setPlacements] = useState(null);
  const [departmentWise, setDepartmentWise] = useState(null);

  useEffect(() => {
    (async () => {
      const { data: p } = await reportApi.placements();
      setPlacements(p);
      const { data: d } = await reportApi.departmentWise();
      setDepartmentWise(d);
    })();
  }, []);

  if (!placements) return <p className="muted">Loading reports…</p>;

  const applicationsData = Object.entries(placements.applicationsByStatus || {}).map(([name, value]) => ({ name, value }));
  const interviewsData = Object.entries(placements.interviewsByStatus || {}).map(([name, value]) => ({ name, value }));
  const departmentData = Object.entries(departmentWise || {}).map(([name, v]) => ({ name, total: v.totalStudents, placed: v.placed }));

  return (
    <div>
      <h1>Placement Reports</h1>
      <div className="card-grid">
        <DashboardCard label="Total Students" value={placements.totalStudents} />
        <DashboardCard label="Placed" value={placements.totalPlaced} />
        <DashboardCard label="Not Placed" value={placements.totalNotPlaced} />
      </div>

      <div className="card-grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
        <div className="card">
          <h3>Applications by Status</h3>
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie data={applicationsData} dataKey="value" nameKey="name" outerRadius={90} label>
                {applicationsData.map((entry, index) => <Cell key={index} fill={COLORS[index % COLORS.length]} />)}
              </Pie>
              <Tooltip /><Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h3>Interviews by Status</h3>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={interviewsData}>
              <XAxis dataKey="name" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#263a68" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card" style={{ marginTop: 24 }}>
        <h3>Department-wise Placement</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={departmentData}>
            <XAxis dataKey="name" tick={{ fontSize: 11 }} />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Legend />
            <Bar dataKey="total" fill="#5a6274" radius={[6, 6, 0, 0]} name="Total Students" />
            <Bar dataKey="placed" fill="#d68c33" radius={[6, 6, 0, 0]} name="Placed" />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
