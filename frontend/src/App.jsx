import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import ProtectedRoute from './routes/ProtectedRoute.jsx';
import Layout from './components/Layout.jsx';

import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';
import StudentDashboard from './pages/StudentDashboard.jsx';
import InterviewerDashboard from './pages/InterviewerDashboard.jsx';
import Students from './pages/Students.jsx';
import Companies from './pages/Companies.jsx';
import RecruitmentDrives from './pages/RecruitmentDrives.jsx';
import DriveDetail from './pages/DriveDetail.jsx';
import Applications from './pages/Applications.jsx';
import InterviewPanels from './pages/InterviewPanels.jsx';
import Availability from './pages/Availability.jsx';
import Scheduler from './pages/Scheduler.jsx';
import InterviewCalendarPage from './pages/InterviewCalendarPage.jsx';
import Feedback from './pages/Feedback.jsx';
import Reports from './pages/Reports.jsx';
import PendingApprovals from './pages/PendingApprovals.jsx';
import AcademicSettingsPage from './pages/AcademicSettingsPage.jsx';
import ChangePassword from './pages/ChangePassword.jsx';

function RoleDashboard() {
  const { user } = useAuth();
  if (user.role === 'ADMIN') return <AdminDashboard />;
  if (user.role === 'STUDENT') return <StudentDashboard />;
  return <InterviewerDashboard />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route path="/" element={
        <ProtectedRoute><Layout><RoleDashboard /></Layout></ProtectedRoute>
      } />

      <Route path="/students" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><Students /></Layout></ProtectedRoute>
      } />

      <Route path="/companies" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><Companies /></Layout></ProtectedRoute>
      } />

      <Route path="/drives" element={
        <ProtectedRoute roles={['ADMIN', 'STUDENT']}><Layout><RecruitmentDrives /></Layout></ProtectedRoute>
      } />

      <Route path="/drives/:id" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><DriveDetail /></Layout></ProtectedRoute>
      } />

      <Route path="/applications" element={
        <ProtectedRoute roles={['STUDENT']}><Layout><Applications /></Layout></ProtectedRoute>
      } />

      <Route path="/panels" element={
        <ProtectedRoute roles={['ADMIN', 'INTERVIEWER']}><Layout><InterviewPanels /></Layout></ProtectedRoute>
      } />

      <Route path="/availability" element={
        <ProtectedRoute roles={['STUDENT', 'INTERVIEWER']}><Layout><Availability /></Layout></ProtectedRoute>
      } />

      <Route path="/scheduler" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><Scheduler /></Layout></ProtectedRoute>
      } />

      <Route path="/calendar" element={
        <ProtectedRoute roles={['ADMIN', 'STUDENT', 'INTERVIEWER']}><Layout><InterviewCalendarPage /></Layout></ProtectedRoute>
      } />

      <Route path="/feedback" element={
        <ProtectedRoute roles={['INTERVIEWER']}><Layout><Feedback /></Layout></ProtectedRoute>
      } />

      <Route path="/reports" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><Reports /></Layout></ProtectedRoute>
      } />

      <Route path="/pending-approvals" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><PendingApprovals /></Layout></ProtectedRoute>
      } />

      <Route path="/academic-settings" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><AcademicSettingsPage /></Layout></ProtectedRoute>
      } />

      <Route path="/change-password" element={
        <ProtectedRoute><Layout><ChangePassword /></Layout></ProtectedRoute>
      } />
    </Routes>
  );
}
