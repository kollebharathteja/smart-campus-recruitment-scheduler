import React, { useEffect, useState } from 'react';
import { availabilityApi, studentApi, interviewerApi } from '../services/api';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';

export default function Availability() {
  const { user } = useAuth();
  const isStudent = user.role === 'STUDENT';
  const [ownerId, setOwnerId] = useState(null);
  const [date, setDate] = useState('');
  const [slots, setSlots] = useState([{ startTime: '10:00', endTime: '12:00' }]);
  const [history, setHistory] = useState([]);
  const { push, errorFromException } = useToast();

  const loadHistory = async (id) => {
    const { data } = isStudent ? await availabilityApi.getStudent(id) : await availabilityApi.getInterviewer(id);
    setHistory(data.sort((a, b) => (a.date < b.date ? -1 : 1)));
  };

  useEffect(() => {
    (async () => {
      const { data } = isStudent ? await studentApi.getMe() : await interviewerApi.getMe();
      setOwnerId(data.id);
      loadHistory(data.id);
    })();
  }, []);

  const updateSlot = (idx, field) => (e) => {
    const next = [...slots];
    next[idx] = { ...next[idx], [field]: e.target.value };
    setSlots(next);
  };

  const addSlot = () => setSlots([...slots, { startTime: '14:00', endTime: '16:00' }]);
  const removeSlot = (idx) => setSlots(slots.filter((_, i) => i !== idx));

  const submit = async (e) => {
    e.preventDefault();
    try {
      const payload = { ownerId, date, slots };
      if (isStudent) await availabilityApi.submitStudent(payload);
      else await availabilityApi.submitInterviewer(payload);
      push('Availability saved.', 'success');
      loadHistory(ownerId);
    } catch (err) {
      errorFromException(err, 'Could not save availability.');
    }
  };

  return (
    <div>
      <h1>My Availability</h1>
      <p className="muted" style={{ marginBottom: 20 }}>Submit the dates and time windows you're free for interviews. The scheduler will only book within these windows.</p>

      <div className="card" style={{ maxWidth: 560, marginBottom: 24 }}>
        <form onSubmit={submit}>
          <div className="form-group">
            <label>Date</label>
            <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
          </div>
          {slots.map((slot, idx) => (
            <div className="form-row" key={idx} style={{ alignItems: 'end' }}>
              <div className="form-group">
                <label>Start time</label>
                <input type="time" value={slot.startTime} onChange={updateSlot(idx, 'startTime')} required />
              </div>
              <div className="form-group" style={{ display: 'flex', gap: 8, alignItems: 'end' }}>
                <div style={{ flex: 1 }}>
                  <label>End time</label>
                  <input type="time" value={slot.endTime} onChange={updateSlot(idx, 'endTime')} required />
                </div>
                {slots.length > 1 && <button type="button" className="btn btn-outline btn-sm" onClick={() => removeSlot(idx)}>✕</button>}
              </div>
            </div>
          ))}
          <button type="button" className="btn btn-outline btn-sm" onClick={addSlot} style={{ marginBottom: 16 }}>+ Add another window</button>
          <div><button className="btn btn-primary">Save Availability</button></div>
        </form>
      </div>

      <h3>Submitted Availability</h3>
      <div className="card">
        <table>
          <thead><tr><th>Date</th><th>Windows</th></tr></thead>
          <tbody>
            {history.map((h) => (
              <tr key={h.id}>
                <td>{h.date}</td>
                <td>{h.slots.map((s, i) => <span key={i} className="badge badge-navy" style={{ marginRight: 6 }}>{s.startTime}–{s.endTime}</span>)}</td>
              </tr>
            ))}
            {history.length === 0 && <tr><td colSpan={2} className="empty-state">No availability submitted yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
