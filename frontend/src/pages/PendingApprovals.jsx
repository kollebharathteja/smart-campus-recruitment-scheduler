import React, { useEffect, useState } from 'react';
import { lecturerApprovalApi, interviewerApi, adminUserApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

export default function PendingApprovals() {
  const [pending, setPending] = useState([]);
  const [lecturers, setLecturers] = useState([]);
  const [loading, setLoading] = useState(true);
  const { push, errorFromException } = useToast();

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await lecturerApprovalApi.getPending();
      setPending(data);
      const { data: allInterviewers } = await interviewerApi.getAll();
      setLecturers(allInterviewers);
    } catch (err) {
      errorFromException(err, 'Could not load lecturer accounts.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const approve = async (userId, name) => {
    try {
      await lecturerApprovalApi.approve(userId);
      push(`${name} approved — they can now sign in.`, 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  const reject = async (userId, name) => {
    if (!confirm(`Reject and delete ${name}'s registration? This cannot be undone.`)) return;
    try {
      await lecturerApprovalApi.reject(userId);
      push(`${name}'s registration was rejected.`, 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  const resetPassword = async (lecturer) => {
    if (!lecturer.userId) {
      push("This lecturer doesn't have a login account.", 'error');
      return;
    }
    const newPassword = prompt(`Set a new password for ${lecturer.name} (min 6 characters):`);
    if (!newPassword) return;
    if (newPassword.length < 6) {
      push('Password must be at least 6 characters.', 'error');
      return;
    }
    try {
      await adminUserApi.resetPassword(lecturer.userId, newPassword);
      push(`Password reset for ${lecturer.name}. Share the new password with them securely.`, 'success');
    } catch (err) {
      errorFromException(err);
    }
  };

  return (
    <div>
      <div className="section-title">
        <h1>Pending Lecturer Approvals</h1>
      </div>
      <p className="muted" style={{ marginBottom: 20 }}>
        Lecturer/interviewer accounts can't sign in until you approve them here.
      </p>
      <div className="card" style={{ marginBottom: 28 }}>
        <table>
          <thead>
            <tr>
              <th>Name</th><th>Email</th><th>Department</th><th>Requested</th><th></th>
            </tr>
          </thead>
          <tbody>
            {pending.map((p) => (
              <tr key={p.userId}>
                <td>{p.name}</td>
                <td>{p.email}</td>
                <td>{p.department || '—'}</td>
                <td>{p.requestedAt ? new Date(p.requestedAt).toLocaleString() : '—'}</td>
                <td style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-primary btn-sm" onClick={() => approve(p.userId, p.name)}>Approve</button>
                  <button className="btn btn-danger btn-sm" onClick={() => reject(p.userId, p.name)}>Reject</button>
                </td>
              </tr>
            ))}
            {!loading && pending.length === 0 && (
              <tr><td colSpan={5} className="empty-state">No pending lecturer registrations.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="section-title">
        <h2>Approved Lecturers</h2>
      </div>
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Name</th><th>Email</th><th>Department</th><th></th>
            </tr>
          </thead>
          <tbody>
            {lecturers.map((l) => (
              <tr key={l.id}>
                <td>{l.name}</td>
                <td>{l.email}</td>
                <td>{l.department || '—'}</td>
                <td><button className="btn btn-outline btn-sm" onClick={() => resetPassword(l)}>Reset Password</button></td>
              </tr>
            ))}
            {!loading && lecturers.length === 0 && (
              <tr><td colSpan={4} className="empty-state">No approved lecturers yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

