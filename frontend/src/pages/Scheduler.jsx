import React, { useEffect, useState } from 'react';
import { driveApi, schedulerApi, studentApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const STRATEGIES = [
  { value: 'FIRST_AVAILABLE', label: 'First Available Valid Slot' },
  { value: 'EARLIEST_AVAILABLE', label: 'Earliest Available Slot' },
  { value: 'STUDENT_PRIORITY', label: 'Student Priority' },
  { value: 'INTERVIEWER_PRIORITY', label: 'Interviewer Priority' },
  { value: 'BALANCED_PANEL_DISTRIBUTION', label: 'Balanced Panel Distribution' }
];

export default function Scheduler() {
  const [drives, setDrives] = useState([]);
  const [selectedDrive, setSelectedDrive] = useState('');
  const [rounds, setRounds] = useState([]);
  const [roundId, setRoundId] = useState('');
  const [dates, setDates] = useState(['']);
  const [duration, setDuration] = useState(30);
  const [strategy, setStrategy] = useState('FIRST_AVAILABLE');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [students, setStudents] = useState({});
  const { push, errorFromException } = useToast();

  useEffect(() => {
    (async () => {
      const { data } = await driveApi.getAll();
      setDrives(data);
      if (data.length) setSelectedDrive(data[0].id);
      const { data: allStudents } = await studentApi.getAll();
      const map = {};
      allStudents.forEach((s) => { map[s.id] = s; });
      setStudents(map);
    })();
  }, []);

  useEffect(() => {
    if (!selectedDrive) return;
    (async () => {
      const { data } = await driveApi.getRounds(selectedDrive);
      setRounds(data);
      if (data.length) { setRoundId(data[0].id); setDuration(data[0].durationMinutes || 30); }
    })();
  }, [selectedDrive]);

  const updateDate = (idx) => (e) => {
    const next = [...dates];
    next[idx] = e.target.value;
    setDates(next);
  };
  const addDate = () => setDates([...dates, '']);
  const removeDate = (idx) => setDates(dates.filter((_, i) => i !== idx));

  const run = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    try {
      const { data } = await schedulerApi.generate({
        driveId: selectedDrive,
        roundId,
        candidateDates: dates.filter(Boolean),
        durationMinutes: parseInt(duration),
        strategy
      });
      setResult(data);
      push(`Scheduled ${data.scheduledInterviewIds.length} interview(s).`, 'success');
    } catch (err) {
      errorFromException(err, 'Scheduling failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1>Automated Clash-Free Scheduler</h1>
      <p className="muted" style={{ marginBottom: 20 }}>
        Matches student and interviewer availability, checks for conflicts, and books the first clash-free slot for every shortlisted candidate in the selected round.
      </p>

      <div className="card" style={{ maxWidth: 640, marginBottom: 24 }}>
        <form onSubmit={run}>
          <div className="form-row">
            <div className="form-group">
              <label>Recruitment drive</label>
              <select value={selectedDrive} onChange={(e) => setSelectedDrive(e.target.value)}>
                {drives.map((d) => <option key={d.id} value={d.id}>{d.jobRole}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Round</label>
              <select value={roundId} onChange={(e) => setRoundId(e.target.value)}>
                {rounds.map((r) => <option key={r.id} value={r.id}>{r.roundName}</option>)}
              </select>
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Interview duration (minutes)</label>
              <input type="number" value={duration} onChange={(e) => setDuration(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Scheduling strategy</label>
              <select value={strategy} onChange={(e) => setStrategy(e.target.value)}>
                {STRATEGIES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>Candidate dates (tried in order)</label>
            {dates.map((d, idx) => (
              <div key={idx} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                <input type="date" value={d} onChange={updateDate(idx)} />
                {dates.length > 1 && <button type="button" className="btn btn-outline btn-sm" onClick={() => removeDate(idx)}>✕</button>}
              </div>
            ))}
            <button type="button" className="btn btn-outline btn-sm" onClick={addDate}>+ Add date</button>
          </div>
          <button className="btn btn-amber" disabled={loading || !roundId}>{loading ? 'Scheduling…' : 'Generate Clash-Free Schedule'}</button>
        </form>
      </div>

      {result && (
        <div className="card">
          <h3>Scheduled ({result.scheduledInterviewIds.length})</h3>
          <p className="muted">Interviews created: {result.scheduledInterviewIds.join(', ') || 'none'}</p>

          <h3 style={{ marginTop: 20 }}>Unscheduled Candidates ({result.unscheduled.length})</h3>
          <table>
            <thead><tr><th>Student</th><th>Status</th><th>Reason</th></tr></thead>
            <tbody>
              {result.unscheduled.map((u) => (
                <tr key={u.studentId}>
                  <td>{u.studentName}</td>
                  <td><StatusBadge status="NOT_SCHEDULED" /></td>
                  <td>{u.reason}</td>
                </tr>
              ))}
              {result.unscheduled.length === 0 && <tr><td colSpan={3} className="empty-state">Everyone was scheduled successfully.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
