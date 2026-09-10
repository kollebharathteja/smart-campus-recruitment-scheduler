import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

const NAV = {
  ADMIN: [
    { to: '/', label: 'Dashboard' },
    { to: '/students', label: 'Students' },
    { to: '/companies', label: 'Companies' },
    { to: '/drives', label: 'Recruitment Drives' },
    { to: '/panels', label: 'Interview Panels' },
    { to: '/scheduler', label: 'Scheduler' },
    { to: '/calendar', label: 'Interview Calendar' },
    { to: '/reports', label: 'Reports' },
    { to: '/pending-approvals', label: 'Pending Approvals' },
    { to: '/academic-settings', label: 'Academic Settings' }
  ],
  STUDENT: [
    { to: '/', label: 'Dashboard' },
    { to: '/drives', label: 'Recruitment Drives' },
    { to: '/applications', label: 'My Applications' },
    { to: '/availability', label: 'My Availability' },
    { to: '/calendar', label: 'Interview Calendar' }
  ],
  INTERVIEWER: [
    { to: '/', label: 'Dashboard' },
    { to: '/panels', label: 'My Panels' },
    { to: '/availability', label: 'My Availability' },
    { to: '/calendar', label: 'Interview Calendar' },
    { to: '/feedback', label: 'Interview Feedback' }
  ]
};

export default function Sidebar() {
  const { user } = useAuth();
  const items = NAV[user?.role] || [];

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">Smart Campus<br /><span>Recruitment &amp; Scheduler</span></div>
      <nav>
        {items.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
