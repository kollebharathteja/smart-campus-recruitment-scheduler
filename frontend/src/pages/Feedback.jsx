import React, { useEffect, useState } from 'react';
import { interviewApi, interviewerApi, studentApi, feedbackApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const emptyForm = { technicalScore: 5, communicationScore: 5, problemSolvingScore: 5, codingScore: 5, comments: '', decision: 'NEXT_ROUND' };

export default function Feedback() {
  const [interviews, setInterviews] = useState([]);
  const [students, setStudents] = useState({});
  const [active, setActive] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const { push, errorFromException } = useToast();

  const load = async () => {
    const { data: me } = await interviewerApi.getMe();
    const { data } = await interviewApi.getByInterviewer(me.id);
    setInterviews(data);
    const { data: allStudents } = await studentApi.getAll();
    const map = {};
    allStudents.forEach((s) => { map[s.id] = s; });
    setStudents(map);
  };

  useEffect(() => { load(); }, []);

  const pending = interviews.filter((i) => ['SCHEDULED', 'CONFIRMED', 'IN_PROGRESS'].includes(i.status));
  const done = interviews.filter((i) => i.status === 'COMPLETED');

  const submit = async (e) => {
    e.preventDefault();
    try {
      await feedbackApi.submit(active.id, {
        ...form,
        technicalScore: parseInt(form.technicalScore),
        communicationScore: parseInt(form.communicationScore),
        problemSolvingScore: parseInt(form.problemSolvingScore),
        codingScore: parseInt(form.codingScore)
      });
      push('Feedback submitted.', 'success');
      setActive(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      errorFromException(err, 'Could not submit feedback.');
    }
  };

  return (
    <div>
      <h1>Interview Feedback</h1>

      <h3 style={{ marginTop: 20 }}>Pending Feedback ({pending.length})</h3>
      <div className="card">
        <table>
          <thead><tr><th>Candidate</th><th>Date</th><th>Time</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {pending.map((i) => (
              <tr key={i.id}>
                <td>{students[i.studentId]?.name || i.studentId}</td>
                <td>{i.date}</td>
                <td>{i.startTime} – {i.endTime}</td>
                <td><StatusBadge status={i.status} /></td>
                <td><button className="btn btn-primary btn-sm" onClick={() => setActive(i)}>Give Feedback</button></td>
              </tr>
            ))}
            {pending.length === 0 && <tr><td colSpan={5} className="empty-state">Nothing pending.</td></tr>}
          </tbody>
        </table>
      </div>

      <h3 style={{ marginTop: 24 }}>Completed ({done.length})</h3>
      <div className="card">
        <table>
          <thead><tr><th>Candidate</th><th>Date</th><th>Status</th></tr></thead>
          <tbody>
            {done.map((i) => (
              <tr key={i.id}>
                <td>{students[i.studentId]?.name || i.studentId}</td>
                <td>{i.date}</td>
                <td><StatusBadge status={i.status} /></td>
              </tr>
            ))}
            {done.length === 0 && <tr><td colSpan={3} className="empty-state">No completed interviews yet.</td></tr>}
          </tbody>
        </table>
      </div>

      {active && (
        <div className="modal-backdrop" onClick={() => setActive(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Feedback for {students[active.studentId]?.name}</h3>
            <form onSubmit={submit}>
              <div className="form-row">
                <div className="form-group"><label>Technical (1-10)</label><input type="number" min="1" max="10" value={form.technicalScore} onChange={(e) => setForm({ ...form, technicalScore: e.target.value })} /></div>
                <div className="form-group"><label>Communication (1-10)</label><input type="number" min="1" max="10" value={form.communicationScore} onChange={(e) => setForm({ ...form, communicationScore: e.target.value })} /></div>
              </div>
              <div className="form-row">
                <div className="form-group"><label>Problem Solving (1-10)</label><input type="number" min="1" max="10" value={form.problemSolvingScore} onChange={(e) => setForm({ ...form, problemSolvingScore: e.target.value })} /></div>
                <div className="form-group"><label>Coding (1-10)</label><input type="number" min="1" max="10" value={form.codingScore} onChange={(e) => setForm({ ...form, codingScore: e.target.value })} /></div>
              </div>
              <div className="form-group"><label>Comments</label><textarea rows={3} value={form.comments} onChange={(e) => setForm({ ...form, comments: e.target.value })} /></div>
              <div className="form-group">
                <label>Decision</label>
                <select value={form.decision} onChange={(e) => setForm({ ...form, decision: e.target.value })}>
                  <option value="NEXT_ROUND">Move to Next Round</option>
                  <option value="SELECTED">Select Candidate</option>
                  <option value="REJECTED">Reject Candidate</option>
                  <option value="ON_HOLD">Put On Hold</option>
                </select>
              </div>
              <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-outline" onClick={() => setActive(null)}>Cancel</button>
                <button className="btn btn-primary">Submit Feedback</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
