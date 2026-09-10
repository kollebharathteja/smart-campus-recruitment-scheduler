import React, { useEffect, useState } from 'react';
import DashboardCard from '../components/DashboardCard.jsx';
import { studentApi, applicationApi, interviewApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

export default function StudentDashboard() {
  const [stats, setStats] = useState(null);
  const [profile, setProfile] = useState(null);
  const [phone, setPhone] = useState('');
  const [resumeUrl, setResumeUrl] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);
  const { push, errorFromException } = useToast();

  const loadProfile = async () => {
    const { data: student } = await studentApi.getMe();
    setProfile(student);
    setPhone(student.phone || '');
    setResumeUrl(student.resumeUrl || '');
    return student;
  };

  useEffect(() => {
    (async () => {
      const student = await loadProfile();
      const { data: applications } = await applicationApi.getByStudent(student.id);
      const { data: interviews } = await interviewApi.getByStudent(student.id);
      setStats({
        applied: applications.length,
        shortlisted: applications.filter((a) => a.status === 'SHORTLISTED' || a.status === 'IN_PROCESS').length,
        upcoming: interviews.filter((i) => i.status === 'SCHEDULED' || i.status === 'CONFIRMED').length,
        completed: interviews.filter((i) => i.status === 'COMPLETED').length,
        status: student.placementStatus
      });
    })();
  }, []);

  const saveBasicDetails = async (e) => {
    e.preventDefault();
    setSavingProfile(true);
    try {
      await studentApi.updateMyProfile({ phone, resumeUrl });
      push('Contact details updated.', 'success');
      loadProfile();
    } catch (err) {
      errorFromException(err, 'Could not update your details.');
    } finally {
      setSavingProfile(false);
    }
  };

  if (!stats || !profile) return <p className="muted">Loading dashboard…</p>;

  const profileFields = [profile.rollNumber, profile.department, profile.degree, profile.cgpa, profile.graduationYear, profile.resumeUrl];
  const completion = Math.round((profileFields.filter(Boolean).length / profileFields.length) * 100);

  return (
    <div>
      <h1>Welcome, {profile.name.split(' ')[0]}</h1>
      <p className="muted" style={{ marginBottom: 24 }}>Track your recruitment journey here.</p>
      <div className="card-grid">
        <DashboardCard label="Profile Completion" value={`${completion}%`} />
        <DashboardCard label="Applied Drives" value={stats.applied} />
        <DashboardCard label="Shortlisted" value={stats.shortlisted} />
        <DashboardCard label="Upcoming Interviews" value={stats.upcoming} />
        <DashboardCard label="Completed Interviews" value={stats.completed} />
        <DashboardCard label="Placement Status" value={stats.status?.replace('_', ' ')} />
      </div>

      <div className="card-grid" style={{ gridTemplateColumns: '1fr 1fr', marginTop: 8 }}>
        <div className="card">
          <h3 style={{ marginBottom: 4 }}>My Eligibility Record</h3>
          <p className="muted" style={{ marginBottom: 14 }}>Set by your T&amp;P office — you can't edit these. Contact admin if anything looks wrong.</p>
          <table>
            <tbody>
              <tr><td className="muted">Roll number</td><td>{profile.rollNumber}</td></tr>
              <tr><td className="muted">Department</td><td>{profile.department}</td></tr>
              <tr><td className="muted">Degree</td><td>{profile.degree}</td></tr>
              <tr><td className="muted">CGPA</td><td>{profile.cgpa}</td></tr>
              <tr><td className="muted">Backlogs</td><td>{profile.backlogs}</td></tr>
              <tr><td className="muted">Graduation year</td><td>{profile.graduationYear}</td></tr>
              <tr><td className="muted">Skills</td><td>{(profile.skills || []).join(', ') || '—'}</td></tr>
              {Object.entries(profile.additionalDetails || {}).map(([k, v]) => (
                <tr key={k}><td className="muted">{k}</td><td>{v}</td></tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 4 }}>My Contact Details</h3>
          <p className="muted" style={{ marginBottom: 14 }}>You can update these yourself — they don't affect your eligibility.</p>
          <form onSubmit={saveBasicDetails}>
            <div className="form-group">
              <label>Phone</label>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Your phone number" />
            </div>
            <div className="form-group">
              <label>Resume link</label>
              <input value={resumeUrl} onChange={(e) => setResumeUrl(e.target.value)} placeholder="Link to your resume (Google Drive, etc.)" />
            </div>
            <button className="btn btn-primary" disabled={savingProfile}>{savingProfile ? 'Saving…' : 'Save'}</button>
          </form>
        </div>
      </div>
    </div>
  );
}
