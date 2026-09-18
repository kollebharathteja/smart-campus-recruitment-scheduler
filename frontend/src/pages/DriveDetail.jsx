import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { driveApi, applicationApi, studentApi, roundResultApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import RoundMarksPanel from '../components/RoundMarksPanel.jsx';
import DriveResultsTable from '../components/DriveResultsTable.jsx';

const TABS = ['Requirements', 'Eligibility', 'Applications', 'Rounds', 'Round Marks', 'Results'];

export default function DriveDetail() {
  const { id } = useParams();
  const [drive, setDrive] = useState(null);
  const [tab, setTab] = useState('Requirements');
  const [eligible, setEligible] = useState([]);
  const [ineligible, setIneligible] = useState([]);
  const [applications, setApplications] = useState([]);
  const [students, setStudents] = useState({});
  const [rounds, setRounds] = useState([]);
  const [roundForm, setRoundForm] = useState({ roundName: '', sequence: 1, durationMinutes: 30 });
  const [activeRoundId, setActiveRoundId] = useState('');
  const [results, setResults] = useState([]);
  const { push, errorFromException } = useToast();

  const load = async () => {
    const { data } = await driveApi.getById(id);
    setDrive(data);
    const { data: allStudents } = await studentApi.getAll();
    const map = {};
    allStudents.forEach((s) => { map[s.id] = s; });
    setStudents(map);
    const { data: r } = await driveApi.getRounds(id);
    setRounds(r);
    const { data: apps } = await applicationApi.getByDrive(id);
    setApplications(apps);
    setActiveRoundId((current) => (r.some((x) => x.id === current) ? current : r[0]?.id || ''));
  };

  const loadResults = async () => {
    const { data } = await roundResultApi.driveResults(id);
    setResults(data);
  };

  const loadEligibility = async () => {
    const { data: e } = await driveApi.eligibleStudents(id);
    setEligible(e);
    const { data: ie } = await driveApi.ineligibleStudents(id);
    setIneligible(ie);
  };

  useEffect(() => { load(); loadEligibility(); loadResults(); }, [id]);

  const shortlist = async (applicationId) => {
    try {
      await applicationApi.shortlist(applicationId);
      push('Candidate shortlisted — login created if needed and an email was sent.', 'success');
      load();
    } catch (err) { errorFromException(err); }
  };

  const shortlistAll = async () => {
    try {
      await applicationApi.shortlistAll(id);
      push('All eligible candidates shortlisted — logins created where needed and emails sent.', 'success');
      load();
      loadEligibility();
    } catch (err) { errorFromException(err); }
  };

  const reject = async (applicationId) => {
    try {
      await applicationApi.reject(applicationId);
      push('Application rejected.', 'success');
      load();
    } catch (err) { errorFromException(err); }
  };

  const addRound = async (e) => {
    e.preventDefault();
    try {
      await driveApi.addRound(id, roundForm);
      push('Round added.', 'success');
      setRoundForm({ roundName: '', sequence: rounds.length + 2, durationMinutes: 30 });
      load();
    } catch (err) { errorFromException(err); }
  };

  const deleteRound = async (roundId) => {
    if (!confirm('Delete this round?')) return;
    try {
      await driveApi.deleteRound(roundId);
      load();
    } catch (err) { errorFromException(err); }
  };

  const activeRound = rounds.find((r) => r.id === activeRoundId) || null;

  if (!drive) return <p className="muted">Loading…</p>;

  return (
    <div>
      <h1>{drive.jobRole}</h1>
      <p className="muted" style={{ marginBottom: 20 }}>{drive.description}</p>

      <div className="pill-tabs">
        {TABS.map((t) => (
          <button key={t} className={`pill-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>

      {tab === 'Requirements' && (
        <div className="card">
          <h3>Dynamic Requirements</h3>
          <table>
            <tbody>
              <tr><td>Minimum CGPA</td><td>{drive.requirements?.minimumCgpa}</td></tr>
              <tr><td>Maximum backlogs</td><td>{drive.requirements?.maximumBacklogs}</td></tr>
              <tr><td>Eligible degrees</td><td>{(drive.requirements?.eligibleDegrees || []).join(', ') || 'Any'}</td></tr>
              <tr><td>Eligible departments</td><td>{(drive.requirements?.eligibleDepartments || []).join(', ') || 'Any'}</td></tr>
              <tr><td>Graduation year</td><td>{drive.requirements?.graduationYear || 'Any'}</td></tr>
              <tr><td>Required skills</td><td>{(drive.requirements?.requiredSkills || []).join(', ') || '—'}</td></tr>
              <tr>
                <td>Additional criteria (10th %, 12th %, UG/PG CGPA, etc.)</td>
                <td>
                  {(drive.requirements?.customCriteria || []).length === 0
                    ? '—'
                    : drive.requirements.customCriteria.map((c) => `${c.fieldName} ≥ ${c.minimumValue}`).join(', ')}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}

      {tab === 'Eligibility' && (
        <div className="card">
          <div className="section-title">
            <h3>Eligible Students ({eligible.length})</h3>
            <button className="btn btn-amber btn-sm" onClick={shortlistAll}>Shortlist All Eligible</button>
          </div>
          <table>
            <thead><tr><th>Student</th><th>Result</th></tr></thead>
            <tbody>
              {eligible.map((r) => (
                <tr key={r.studentId}><td>{r.studentName}</td><td><StatusBadge status="ELIGIBLE" /></td></tr>
              ))}
            </tbody>
          </table>

          <h3 style={{ marginTop: 24 }}>Not Eligible ({ineligible.length})</h3>
          <table>
            <thead><tr><th>Student</th><th>Reasons</th></tr></thead>
            <tbody>
              {ineligible.map((r) => (
                <tr key={r.studentId}>
                  <td>{r.studentName}</td>
                  <td>
                    <ul style={{ margin: 0, paddingLeft: 16 }}>
                      {r.reasons.map((reason, i) => <li key={i} style={{ fontSize: 13 }}>{reason}</li>)}
                    </ul>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'Applications' && (
        <div className="card">
          <table>
            <thead><tr><th>Student</th><th>Status</th><th>Round</th><th></th></tr></thead>
            <tbody>
              {applications.map((a) => (
                <tr key={a.id}>
                  <td>{students[a.studentId]?.name || a.studentId}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td>{rounds.find((r) => r.id === a.currentRoundId)?.roundName || '—'}</td>
                  <td style={{ display: 'flex', gap: 6 }}>
                    {(a.status === 'APPLIED') && <button className="btn btn-sm btn-primary" onClick={() => shortlist(a.id)}>Shortlist</button>}
                    {a.status !== 'REJECTED' && a.status !== 'SELECTED' && <button className="btn btn-sm btn-danger" onClick={() => reject(a.id)}>Reject</button>}
                  </td>
                </tr>
              ))}
              {applications.length === 0 && <tr><td colSpan={4} className="empty-state">No applications yet.</td></tr>}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'Rounds' && (
        <div className="card">
          <h3>Interview Rounds</h3>
          <table style={{ marginBottom: 20 }}>
            <thead><tr><th>#</th><th>Round</th><th>Duration (min)</th><th></th></tr></thead>
            <tbody>
              {rounds.map((r) => (
                <tr key={r.id}>
                  <td>{r.sequence}</td><td>{r.roundName}</td><td>{r.durationMinutes}</td>
                  <td><button className="btn btn-sm btn-danger" onClick={() => deleteRound(r.id)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <form onSubmit={addRound} className="form-row" style={{ alignItems: 'end' }}>
            <div className="form-group"><label>Round name</label><input value={roundForm.roundName} onChange={(e) => setRoundForm({ ...roundForm, roundName: e.target.value })} required /></div>
            <div className="form-group"><label>Sequence</label><input type="number" value={roundForm.sequence} onChange={(e) => setRoundForm({ ...roundForm, sequence: parseInt(e.target.value) })} required /></div>
            <div className="form-group"><label>Duration (minutes)</label><input type="number" value={roundForm.durationMinutes} onChange={(e) => setRoundForm({ ...roundForm, durationMinutes: parseInt(e.target.value) })} required /></div>
            <div className="form-group"><button className="btn btn-primary">Add Round</button></div>
          </form>
        </div>
      )}

      {tab === 'Round Marks' && (
        <div className="card">
          <div className="section-title">
            <h3>Upload Round Marks</h3>
            <select value={activeRoundId} onChange={(e) => setActiveRoundId(e.target.value)} style={{ maxWidth: 260 }}>
              {rounds.map((r) => <option key={r.id} value={r.id}>{r.sequence}. {r.roundName}</option>)}
            </select>
          </div>
          {activeRound
            ? <RoundMarksPanel round={activeRound} onSaved={() => { load(); loadResults(); }} />
            : <p className="empty-state">Add at least one round before uploading marks.</p>}
        </div>
      )}

      {tab === 'Results' && (
        <div className="card">
          <h3>Round-wise Results &amp; Selections</h3>
          <DriveResultsTable results={results} />
        </div>
      )}
    </div>
  );
}
