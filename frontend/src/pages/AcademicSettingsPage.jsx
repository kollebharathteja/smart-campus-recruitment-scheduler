import React, { useEffect, useState } from 'react';
import { academicSettingsApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

export default function AcademicSettingsPage() {
  const [settings, setSettings] = useState({ departments: [], degrees: [] });
  const [newDept, setNewDept] = useState('');
  const [newDegree, setNewDegree] = useState('');
  const { push, errorFromException } = useToast();

  const load = async () => {
    const { data } = await academicSettingsApi.get();
    setSettings(data);
  };

  useEffect(() => { load(); }, []);

  const addDept = async (e) => {
    e.preventDefault();
    if (!newDept.trim()) return;
    try {
      await academicSettingsApi.addDepartment(newDept.trim());
      setNewDept('');
      push('Department added.', 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  const removeDept = async (name) => {
    if (!confirm(`Remove "${name}" from departments? Existing student/drive records keep their value, but it will disappear from future dropdowns.`)) return;
    try {
      await academicSettingsApi.removeDepartment(name);
      push('Department removed.', 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  const addDegree = async (e) => {
    e.preventDefault();
    if (!newDegree.trim()) return;
    try {
      await academicSettingsApi.addDegree(newDegree.trim());
      setNewDegree('');
      push('Degree added.', 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  const removeDegree = async (name) => {
    if (!confirm(`Remove "${name}" from degrees?`)) return;
    try {
      await academicSettingsApi.removeDegree(name);
      push('Degree removed.', 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  return (
    <div>
      <div className="section-title">
        <h1>Academic Settings</h1>
      </div>
      <p className="muted" style={{ marginBottom: 20 }}>
        These lists feed every department/degree dropdown across the app — student registration, the Students page, and recruitment drive requirements.
      </p>

      <div className="card-grid">
        <div className="card">
          <h3 style={{ marginBottom: 12 }}>Departments</h3>
          <form onSubmit={addDept} style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
            <input placeholder="e.g. Computer Applications" value={newDept} onChange={(e) => setNewDept(e.target.value)} style={{ flex: 1 }} />
            <button className="btn btn-primary btn-sm">Add</button>
          </form>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {settings.departments.map((d) => (
              <div key={d} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid #eee' }}>
                <span>{d}</span>
                <button className="btn btn-danger btn-sm" onClick={() => removeDept(d)}>Remove</button>
              </div>
            ))}
            {settings.departments.length === 0 && <p className="muted">No departments yet.</p>}
          </div>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 12 }}>Degrees</h3>
          <form onSubmit={addDegree} style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
            <input placeholder="e.g. MCA" value={newDegree} onChange={(e) => setNewDegree(e.target.value)} style={{ flex: 1 }} />
            <button className="btn btn-primary btn-sm">Add</button>
          </form>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {settings.degrees.map((d) => (
              <div key={d} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid #eee' }}>
                <span>{d}</span>
                <button className="btn btn-danger btn-sm" onClick={() => removeDegree(d)}>Remove</button>
              </div>
            ))}
            {settings.degrees.length === 0 && <p className="muted">No degrees yet.</p>}
          </div>
        </div>
      </div>
    </div>
  );
}
