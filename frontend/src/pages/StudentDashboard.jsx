import React, { useEffect, useState } from 'react';
import DashboardCard from '../components/DashboardCard.jsx';
import { studentApi, applicationApi, interviewApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

export default function StudentDashboard() {
  const [stats, setStats] = useState(null);
  const [profile, setProfile] = useState(null);
  const [phone, setPhone] = useState('');
  const [resumeUrl, setResumeUrl] = useState('');
  const [detailRows, setDetailRows] = useState([]);
  const [savingProfile, setSavingProfile] = useState(false);
  const { push, errorFromException } = useToast();

  const loadProfile = async () => {
    const { data: student } = await studentApi.getMe();
    setProfile(student);
    setPhone(student.phone || '');
    setResumeUrl(student.resumeUrl || '');
    setDetailRows(Object.entries(student.additionalDetails || {}).map(([key, value]) => ({ key, value: String(value) })));
    return student;
  };

  const addDetailRow = () => setDetailRows((rows) => [...rows, { key: '', value: '' }]);
  const updateDetailRow = (i, field, val) => setDetailRows((rows) => {
    const next = [...rows];
    next[i] = { ...next[i], [field]: val };
    return next;
  });
  const removeDetailRow = (i) => setDetailRows((rows) => rows.filter((_, idx) => idx !== i));

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
    const additionalDetails = {};
    for (const row of detailRows) {
      if (row.key.trim() && row.value !== '' && !Number.isNaN(parseFloat(row.value))) {
        additionalDetails[row.key.trim()] = parseFloat(row.value);
      }
    }
    try {
      await studentApi.updateMyProfile({ phone, resumeUrl, additionalDetails });
      push('Details updated.', 'success');
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
          <p className="muted" style={{ marginBottom: 14 }}>CGPA, backlogs, department, degree, and skills are set by your T&amp;P office — you can't edit these here. Contact admin if anything looks wrong.</p>
          <table>
            <tbody>
              <tr><td className="muted">Roll number</td><td>{profile.rollNumber}</td></tr>
              <tr><td className="muted">Department</td><td>{profile.department}</td></tr>
              <tr><td className="muted">Degree</td><td>{profile.degree}</td></tr>
              <tr><td className="muted">CGPA</td><td>{profile.cgpa}</td></tr>
              <tr><td className="muted">Backlogs</td><td>{profile.backlogs}</td></tr>
              <tr><td className="muted">Graduation year</td><td>{profile.graduationYear}</td></tr>
              <tr><td className="muted">Skills</td><td>{(profile.skills || []).join(', ') || '—'}</td></tr>
            </tbody>
          </table>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 4 }}>My Contact &amp; Academic History</h3>
          <p className="muted" style={{ marginBottom: 14 }}>Self-reported — add things like 10th %, 12th %, UG CGPA, PG CGPA if a drive asks for them. This doesn't touch your official CGPA above.</p>
          <form onSubmit={saveBasicDetails}>
            <div className="form-group">
              <label>Phone</label>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Your phone number" />
            </div>
            <div className="form-group">
              <label>Resume link</label>
              <input value={resumeUrl} onChange={(e) => setResumeUrl(e.target.value)} placeholder="Link to your resume (Google Drive, etc.)" />
            </div>
            <div className="form-group">
              <label>Academic history (optional)</label>
              {detailRows.map((row, i) => (
                <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                  <input placeholder="e.g. 10th Percentage" value={row.key} onChange={(e) => updateDetailRow(i, 'key', e.target.value)} style={{ flex: 2 }} />
                  <input type="number" step="0.1" placeholder="Value" value={row.value} onChange={(e) => updateDetailRow(i, 'value', e.target.value)} style={{ flex: 1 }} />
                  <button type="button" className="btn btn-outline btn-sm" onClick={() => removeDetailRow(i)}>✕</button>
                </div>
              ))}
              <button type="button" className="btn btn-outline btn-sm" onClick={addDetailRow}>+ Add detail</button>
            </div>
            <button className="btn btn-primary" disabled={savingProfile} style={{ marginTop: 10 }}>{savingProfile ? 'Saving…' : 'Save'}</button>
          </form>
        </div>
      </div>
    </div>
  );
}
