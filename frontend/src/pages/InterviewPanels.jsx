import React, { useEffect, useState } from 'react';
import { panelApi, driveApi, interviewerApi } from '../services/api';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';

export default function InterviewPanels() {
  const { user } = useAuth();
  const [drives, setDrives] = useState([]);
  const [selectedDrive, setSelectedDrive] = useState('');
  const [rounds, setRounds] = useState([]);
  const [interviewers, setInterviewers] = useState([]);
  const [panels, setPanels] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ panelName: '', roundId: '', venue: '', interviewerIds: [] });
  const { push, errorFromException } = useToast();

  useEffect(() => {
    (async () => {
      const { data: allInterviewers } = await interviewerApi.getAll();
      setInterviewers(allInterviewers);

      if (user.role === 'ADMIN') {
        const { data: allDrives } = await driveApi.getAll();
        setDrives(allDrives);
        if (allDrives.length > 0) setSelectedDrive(allDrives[0].id);
      } else if (user.role === 'INTERVIEWER') {
        const { data: me } = await interviewerApi.getMe();
        const { data: myPanels } = await panelApi.getByInterviewer(me.id);
        setPanels(myPanels);
      }
    })();
  }, []);

  useEffect(() => {
    if (!selectedDrive) return;
    (async () => {
      const { data: r } = await driveApi.getRounds(selectedDrive);
      setRounds(r);
      const { data: p } = await panelApi.getByDrive(selectedDrive);
      setPanels(p);
    })();
  }, [selectedDrive]);

  const toggleInterviewer = (id) => {
    setForm((f) => ({
      ...f,
      interviewerIds: f.interviewerIds.includes(id) ? f.interviewerIds.filter((x) => x !== id) : [...f.interviewerIds, id]
    }));
  };

  const submit = async (e) => {
    e.preventDefault();
    try {
      await panelApi.create({ ...form, driveId: selectedDrive, status: 'ACTIVE' });
      push('Panel created.', 'success');
      setShowForm(false);
      setForm({ panelName: '', roundId: '', venue: '', interviewerIds: [] });
      const { data: p } = await panelApi.getByDrive(selectedDrive);
      setPanels(p);
    } catch (err) { errorFromException(err); }
  };

  const interviewerName = (id) => interviewers.find((i) => i.id === id)?.name || id;
  const roundName = (id) => rounds.find((r) => r.id === id)?.roundName || id;

  return (
    <div>
      <div className="section-title">
        <h1>{user.role === 'ADMIN' ? 'Interview Panels' : 'My Panels'}</h1>
        {user.role === 'ADMIN' && <button className="btn btn-amber" onClick={() => setShowForm(true)} disabled={!selectedDrive}>+ New Panel</button>}
      </div>

      {user.role === 'ADMIN' && (
        <div className="form-group" style={{ maxWidth: 320 }}>
          <label>Recruitment drive</label>
          <select value={selectedDrive} onChange={(e) => setSelectedDrive(e.target.value)}>
            {drives.map((d) => <option key={d.id} value={d.id}>{d.jobRole}</option>)}
          </select>
        </div>
      )}

      <div className="card-grid">
        {panels.map((p) => (
          <div className="card" key={p.id}>
            <h3>{p.panelName}</h3>
            <p className="muted" style={{ fontSize: 13 }}>Round: {roundName(p.roundId)}</p>
            <p className="muted" style={{ fontSize: 13 }}>Venue: {p.venue || '—'}</p>
            <p style={{ fontSize: 14, marginTop: 8 }}>Interviewers: {p.interviewerIds.map(interviewerName).join(', ')}</p>
          </div>
        ))}
        {panels.length === 0 && <p className="muted">No panels yet.</p>}
      </div>

      {showForm && (
        <div className="modal-backdrop" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>New Interview Panel</h3>
            <form onSubmit={submit}>
              <div className="form-group"><label>Panel name</label><input value={form.panelName} onChange={(e) => setForm({ ...form, panelName: e.target.value })} required /></div>
              <div className="form-group">
                <label>Round</label>
                <select value={form.roundId} onChange={(e) => setForm({ ...form, roundId: e.target.value })} required>
                  <option value="">Select…</option>
                  {rounds.map((r) => <option key={r.id} value={r.id}>{r.roundName}</option>)}
                </select>
              </div>
              <div className="form-group"><label>Venue</label><input value={form.venue} onChange={(e) => setForm({ ...form, venue: e.target.value })} /></div>
              <div className="form-group">
                <label>Interviewers</label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {interviewers.map((i) => (
                    <label key={i.id} style={{ display: 'flex', alignItems: 'center', gap: 6, border: '1px solid var(--line)', borderRadius: 8, padding: '6px 10px', fontSize: 13, fontWeight: 400 }}>
                      <input type="checkbox" checked={form.interviewerIds.includes(i.id)} onChange={() => toggleInterviewer(i.id)} />
                      {i.name}
                    </label>
                  ))}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 10 }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowForm(false)}>Cancel</button>
                <button className="btn btn-primary">Create Panel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
