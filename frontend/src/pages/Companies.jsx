import React, { useEffect, useState } from 'react';
import { companyApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

const emptyForm = { name: '', description: '', website: '', hrContactName: '', hrContactEmail: '' };

export default function Companies() {
  const [companies, setCompanies] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const { push, errorFromException } = useToast();

  const load = async () => {
    const { data } = await companyApi.getAll();
    setCompanies(data);
  };

  useEffect(() => { load(); }, []);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const openAdd = () => {
    setEditingId(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (c) => {
    setEditingId(c.id);
    setForm({
      name: c.name || '',
      description: c.description || '',
      website: c.website || '',
      hrContactName: c.hrContactName || '',
      hrContactEmail: c.hrContactEmail || ''
    });
    setShowForm(true);
  };

  const submit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await companyApi.update(editingId, form);
        push('Company updated.', 'success');
      } else {
        await companyApi.create(form);
        push('Company added.', 'success');
      }
      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      errorFromException(err, editingId ? 'Could not update company.' : 'Could not add company.');
    }
  };

  const remove = async (id) => {
    if (!confirm('Delete this company?')) return;
    try {
      await companyApi.delete(id);
      push('Company deleted.', 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  return (
    <div>
      <div className="section-title">
        <h1>Companies</h1>
        <button className="btn btn-amber" onClick={openAdd}>+ Add Company</button>
      </div>
      <div className="card-grid">
        {companies.map((c) => (
          <div className="card" key={c.id}>
            <h3 style={{ marginBottom: 6 }}>{c.name}</h3>
            <p className="muted" style={{ marginBottom: 10 }}>{c.description}</p>
            <p className="muted" style={{ fontSize: 13 }}>HR: {c.hrContactName} ({c.hrContactEmail})</p>
            <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
              <button className="btn btn-outline btn-sm" onClick={() => openEdit(c)}>Edit</button>
              <button className="btn btn-danger btn-sm" onClick={() => remove(c.id)}>Delete</button>
            </div>
          </div>
        ))}
        {companies.length === 0 && <p className="muted">No companies added yet.</p>}
      </div>

      {showForm && (
        <div className="modal-backdrop" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editingId ? 'Edit Company' : 'Add Company'}</h3>
            <form onSubmit={submit}>
              <div className="form-group"><label>Company name</label><input value={form.name} onChange={update('name')} required /></div>
              <div className="form-group"><label>Description</label><textarea rows={3} value={form.description} onChange={update('description')} /></div>
              <div className="form-group"><label>Website</label><input value={form.website} onChange={update('website')} /></div>
              <div className="form-row">
                <div className="form-group"><label>HR contact name</label><input value={form.hrContactName} onChange={update('hrContactName')} /></div>
                <div className="form-group"><label>HR contact email</label><input type="email" value={form.hrContactEmail} onChange={update('hrContactEmail')} /></div>
              </div>
              <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowForm(false)}>Cancel</button>
                <button className="btn btn-primary">{editingId ? 'Save changes' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
