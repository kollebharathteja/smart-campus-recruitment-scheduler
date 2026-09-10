import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { driveApi, companyApi, applicationApi, studentApi, academicSettingsApi } from '../services/api';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const emptyForm = {
  companyId: '', jobRole: '', description: '', applicationDeadline: '', driveDate: '',
  minimumCgpa: '', maximumBacklogs: '', eligibleDegrees: [], eligibleDepartments: [], graduationYear: '',
  requiredSkills: '', minimumSkillMatchPercent: 100, customCriteria: []
};

export default function RecruitmentDrives() {
  const [drives, setDrives] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [myApplications, setMyApplications] = useState([]);
  const [academicOptions, setAcademicOptions] = useState({ departments: [], degrees: [] });
  const { user } = useAuth();
  const { push, errorFromException } = useToast();

  const load = async () => {
    const { data } = await driveApi.getAll();
    setDrives(data);
    const { data: comps } = await companyApi.getAll();
    setCompanies(comps);
  };

  const loadMyApplications = async () => {
    if (user.role !== 'STUDENT') return;
    const meResp = await studentApi.getMe();
    const { data: apps } = await applicationApi.getByStudent(meResp.data.id);
    setMyApplications(apps);
  };

  useEffect(() => {
    load();
    loadMyApplications();
    academicSettingsApi.get().then(({ data }) => setAcademicOptions(data)).catch(() => {});
  }, []);

  const companyName = (id) => companies.find((c) => c.id === id)?.name || '—';

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const toggleInList = (field, value) => {
    setForm((prev) => {
      const list = prev[field].includes(value) ? prev[field].filter((v) => v !== value) : [...prev[field], value];
      return { ...prev, [field]: list };
    });
  };

  const addCriterion = () => setForm((f) => ({ ...f, customCriteria: [...f.customCriteria, { fieldName: '', minimumValue: '' }] }));
  const updateCriterion = (i, field, val) => setForm((f) => {
    const rows = [...f.customCriteria];
    rows[i] = { ...rows[i], [field]: val };
    return { ...f, customCriteria: rows };
  });
  const removeCriterion = (i) => setForm((f) => ({ ...f, customCriteria: f.customCriteria.filter((_, idx) => idx !== i) }));

  const submit = async (e) => {
    e.preventDefault();
    try {
      await driveApi.create({
        companyId: form.companyId,
        jobRole: form.jobRole,
        description: form.description,
        applicationDeadline: form.applicationDeadline,
        driveDate: form.driveDate,
        status: 'OPEN',
        requirements: {
          minimumCgpa: parseFloat(form.minimumCgpa) || 0,
          maximumBacklogs: parseInt(form.maximumBacklogs) || 0,
          eligibleDegrees: form.eligibleDegrees,
          eligibleDepartments: form.eligibleDepartments,
          graduationYear: form.graduationYear ? parseInt(form.graduationYear) : null,
          requiredSkills: form.requiredSkills.split(',').map((s) => s.trim()).filter(Boolean),
          minimumSkillMatchPercent: parseInt(form.minimumSkillMatchPercent) || 100,
          customCriteria: form.customCriteria
            .filter((c) => c.fieldName.trim() && c.minimumValue !== '')
            .map((c) => ({ fieldName: c.fieldName.trim(), minimumValue: parseFloat(c.minimumValue) }))
        }
      });
      push('Recruitment drive created.', 'success');
      setShowForm(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      errorFromException(err, 'Could not create drive.');
    }
  };

  const applicationFor = (driveId) => myApplications.find((a) => a.driveId === driveId);

  const apply = async (driveId) => {
    try {
      const { data: me } = await studentApi.getMe();
      await applicationApi.apply(me.id, driveId);
      push('Application submitted.', 'success');
      loadMyApplications();
    } catch (err) {
      errorFromException(err, 'Could not apply.');
    }
  };

  return (
    <div>
      <div className="section-title">
        <h1>Recruitment Drives</h1>
        {user.role === 'ADMIN' && <button className="btn btn-amber" onClick={() => setShowForm(true)}>+ New Drive</button>}
      </div>

      <div className="card-grid">
        {drives.map((d) => {
          const application = applicationFor(d.id);
          return (
            <div className="card" key={d.id}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h3 style={{ marginBottom: 2 }}>{d.jobRole}</h3>
                  <p className="muted">{companyName(d.companyId)}</p>
                </div>
                <StatusBadge status={d.status} />
              </div>
              <p style={{ fontSize: 14, margin: '10px 0' }}>{d.description}</p>
              <p className="muted" style={{ fontSize: 13 }}>
                Min CGPA: {d.requirements?.minimumCgpa} · Max Backlogs: {d.requirements?.maximumBacklogs}<br />
                Degrees: {(d.requirements?.eligibleDegrees || []).join(', ') || 'Any'}<br />
                Skills: {(d.requirements?.requiredSkills || []).join(', ') || '—'}<br />
                Deadline: {d.applicationDeadline}
              </p>
              <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap' }}>
                {user.role === 'ADMIN' && <Link className="btn btn-outline btn-sm" to={`/drives/${d.id}`}>Manage</Link>}
                {user.role === 'STUDENT' && !application && (
                  <button className="btn btn-primary btn-sm" onClick={() => apply(d.id)}>Check Eligibility &amp; Apply</button>
                )}
                {user.role === 'STUDENT' && application && (
                  <div>
                    <StatusBadge status={application.status} />
                    {application.status === 'NOT_ELIGIBLE' && application.eligibilityReasons?.length > 0 && (
                      <ul style={{ marginTop: 8, paddingLeft: 18, fontSize: 13, color: 'var(--red)' }}>
                        {application.eligibilityReasons.map((r, i) => <li key={i}>{r}</li>)}
                      </ul>
                    )}
                  </div>
                )}
              </div>
            </div>
          );
        })}
        {drives.length === 0 && <p className="muted">No recruitment drives yet.</p>}
      </div>

      {showForm && (
        <div className="modal-backdrop" onClick={() => setShowForm(false)}>
          <div className="modal" style={{ width: 560 }} onClick={(e) => e.stopPropagation()}>
            <h3>New Recruitment Drive</h3>
            <form onSubmit={submit}>
              <div className="form-row">
                <div className="form-group">
                  <label>Company</label>
                  <select value={form.companyId} onChange={update('companyId')} required>
                    <option value="">Select…</option>
                    {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>
                <div className="form-group"><label>Job role</label><input value={form.jobRole} onChange={update('jobRole')} required /></div>
              </div>
              <div className="form-group"><label>Description</label><textarea rows={2} value={form.description} onChange={update('description')} /></div>
              <div className="form-row">
                <div className="form-group"><label>Application deadline</label><input type="date" value={form.applicationDeadline} onChange={update('applicationDeadline')} required /></div>
                <div className="form-group"><label>Drive date</label><input type="date" value={form.driveDate} onChange={update('driveDate')} required /></div>
              </div>
              <h4 style={{ marginTop: 8 }}>Dynamic Eligibility Requirements</h4>
              <div className="form-row">
                <div className="form-group"><label>Minimum CGPA</label><input type="number" step="0.1" value={form.minimumCgpa} onChange={update('minimumCgpa')} required /></div>
                <div className="form-group"><label>Maximum backlogs</label><input type="number" value={form.maximumBacklogs} onChange={update('maximumBacklogs')} required /></div>
              </div>
              <div className="form-group">
                <label>Eligible degrees</label>
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                  {academicOptions.degrees.map((d) => (
                    <label key={d} className="checkbox-pill">
                      <input type="checkbox" checked={form.eligibleDegrees.includes(d)} onChange={() => toggleInList('eligibleDegrees', d)} /> {d}
                    </label>
                  ))}
                  {academicOptions.degrees.length === 0 && <span className="muted">No degrees defined yet — add some under Academic Settings.</span>}
                </div>
              </div>
              <div className="form-group">
                <label>Eligible departments (optional — leave all unchecked to allow any)</label>
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                  {academicOptions.departments.map((d) => (
                    <label key={d} className="checkbox-pill">
                      <input type="checkbox" checked={form.eligibleDepartments.includes(d)} onChange={() => toggleInList('eligibleDepartments', d)} /> {d}
                    </label>
                  ))}
                  {academicOptions.departments.length === 0 && <span className="muted">No departments defined yet — add some under Academic Settings.</span>}
                </div>
              </div>
              <div className="form-row">
                <div className="form-group"><label>Graduation year</label><input type="number" value={form.graduationYear} onChange={update('graduationYear')} /></div>
                <div className="form-group"><label>Required skills (comma separated)</label><input value={form.requiredSkills} onChange={update('requiredSkills')} placeholder="Java, Spring Boot, MongoDB" /></div>
              </div>
              <div className="form-group">
                <label>Minimum skill match required: {form.minimumSkillMatchPercent}%</label>
                <input type="range" min="0" max="100" step="10" value={form.minimumSkillMatchPercent} onChange={update('minimumSkillMatchPercent')} style={{ width: '100%' }} />
                <p className="muted">100% = student needs every listed skill. 50% = at least half is enough.</p>
              </div>

              <div className="form-group">
                <label>Additional eligibility criteria (optional)</label>
                <p className="muted" style={{ marginBottom: 8 }}>
                  For things like 10th %, 12th %, UG CGPA, PG CGPA — matched against each student's "Additional details" (set on the Students page). Label must match exactly.
                </p>
                {form.customCriteria.map((c, i) => (
                  <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                    <input placeholder="e.g. 10th Percentage" value={c.fieldName} onChange={(e) => updateCriterion(i, 'fieldName', e.target.value)} style={{ flex: 2 }} />
                    <span className="muted" style={{ alignSelf: 'center' }}>min ≥</span>
                    <input type="number" step="0.1" placeholder="Value" value={c.minimumValue} onChange={(e) => updateCriterion(i, 'minimumValue', e.target.value)} style={{ flex: 1 }} />
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => removeCriterion(i)}>✕</button>
                  </div>
                ))}
                <button type="button" className="btn btn-outline btn-sm" onClick={addCriterion}>+ Add criterion</button>
              </div>

              <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowForm(false)}>Cancel</button>
                <button className="btn btn-primary">Create Drive</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
//
