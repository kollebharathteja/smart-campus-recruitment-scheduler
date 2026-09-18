import React, { useEffect, useMemo, useState } from 'react';
import { driveApi, interviewerApi, roundResultApi } from '../services/api';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import RoundMarksPanel from '../components/RoundMarksPanel.jsx';
import DriveResultsTable from '../components/DriveResultsTable.jsx';

/**
 * Lecturer view of recruitment outcomes. The backend only ever returns students of the
 * lecturer's own department, so the department picker here just narrows what they already
 * have access to (an admin opening this page sees every department).
 */
export default function DepartmentResults() {
  const { user } = useAuth();
  const { errorFromException } = useToast();
  const [results, setResults] = useState([]);
  const [drives, setDrives] = useState([]);
  const [rounds, setRounds] = useState([]);
  const [profileDepartment, setProfileDepartment] = useState('');
  const [department, setDepartment] = useState('');
  const [driveId, setDriveId] = useState('');
  const [roundId, setRoundId] = useState('');
  const [outcome, setOutcome] = useState('');

  const loadResults = async () => {
    try {
      const { data } = await roundResultApi.allResults();
      setResults(data);
    } catch (err) {
      errorFromException(err, 'Could not load results.');
    }
  };

  useEffect(() => {
    loadResults();
    driveApi.getAll().then(({ data }) => setDrives(data)).catch(() => {});
    if (user?.role === 'INTERVIEWER') {
      interviewerApi.getMe()
        .then(({ data }) => {
          setProfileDepartment(data.department || '');
          setDepartment(data.department || '');
        })
        .catch(() => {});
    }
  }, []);

  useEffect(() => {
    if (!driveId) { setRounds([]); setRoundId(''); return; }
    driveApi.getRounds(driveId)
      .then(({ data }) => {
        setRounds(data);
        setRoundId(data[0]?.id || '');
      })
      .catch(() => setRounds([]));
  }, [driveId]);

  const departments = useMemo(
    () => [...new Set(results.map((r) => r.department).filter(Boolean))],
    [results]
  );

  const filtered = useMemo(() => results.filter((r) => {
    const matchesDept = !department || r.department === department;
    const matchesDrive = !driveId || r.driveId === driveId;
    const matchesOutcome = !outcome
      || (outcome === 'SELECTED' ? r.status === 'SELECTED' : r.status !== 'SELECTED');
    return matchesDept && matchesDrive && matchesOutcome;
  }), [results, department, driveId, outcome]);

  const selectedCount = filtered.filter((r) => r.status === 'SELECTED').length;
  const activeRound = rounds.find((r) => r.id === roundId) || null;

  return (
    <div>
      <div className="section-title">
        <h1>Department Results</h1>
      </div>
      {profileDepartment && (
        <p className="muted" style={{ marginBottom: 16 }}>
          You are viewing students of your department ({profileDepartment}) only.
        </p>
      )}

      <div className="card" style={{ marginBottom: 16, display: 'flex', gap: 14, alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label>Department</label>
          <select value={department} onChange={(e) => setDepartment(e.target.value)}>
            <option value="">All departments</option>
            {departments.map((d) => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label>Company / drive</label>
          <select value={driveId} onChange={(e) => setDriveId(e.target.value)}>
            <option value="">All drives</option>
            {drives.map((d) => <option key={d.id} value={d.id}>{d.jobRole}</option>)}
          </select>
        </div>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label>Outcome</label>
          <select value={outcome} onChange={(e) => setOutcome(e.target.value)}>
            <option value="">All</option>
            <option value="SELECTED">Selected</option>
            <option value="NOT_SELECTED">Not selected</option>
          </select>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h3>{filtered.length} student(s) · {selectedCount} selected</h3>
        <DriveResultsTable results={filtered} showCompany />
      </div>

      {driveId && (
        <div className="card">
          <div className="section-title">
            <h3>Upload Round Marks</h3>
            <select value={roundId} onChange={(e) => setRoundId(e.target.value)} style={{ maxWidth: 260 }}>
              {rounds.map((r) => <option key={r.id} value={r.id}>{r.sequence}. {r.roundName}</option>)}
            </select>
          </div>
          {activeRound
            ? <RoundMarksPanel round={activeRound} onSaved={loadResults} />
            : <p className="empty-state">This drive has no rounds configured yet.</p>}
        </div>
      )}
    </div>
  );
}
