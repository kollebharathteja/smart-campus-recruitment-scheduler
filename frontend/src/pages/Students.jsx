import React, { useEffect, useMemo, useState, useRef } from 'react';
import * as XLSX from 'xlsx';
import { studentApi, academicSettingsApi, adminUserApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

const emptyForm = { name: '', email: '', rollNumber: '', department: '', degree: '', cgpa: '', backlogs: 0, graduationYear: 2027, skills: '', detailRows: [] };

// Column headers expected in the Excel template. Keep this in sync with the
// template generator below and with parseExcelRow().
const EXCEL_COLUMNS = ['Name', 'Email', 'Roll Number', 'Department', 'Degree', 'CGPA', 'Backlogs', 'Graduation Year', 'Skills (comma separated)'];

// Headers that map to a fixed Student field — everything else in the sheet
// becomes a flexible additionalDetails entry (10th %, UG CGPA, PG CGPA, etc.)
const KNOWN_HEADER_KEYS = new Set([
  'name', 'email', 'roll number', 'rollnumber', 'department', 'degree',
  'cgpa', 'backlogs', 'graduation year', 'graduationyear',
  'skills (comma separated)', 'skills'
]);

function downloadTemplate() {
  const sample = [
    ['Rahul Sharma', 'rahul.sharma@example.edu', 'MCA2027010', 'Computer Applications', 'MCA', 8.2, 0, 2027, 'Java, Spring Boot, MongoDB']
  ];
  const ws = XLSX.utils.aoa_to_sheet([EXCEL_COLUMNS, ...sample]);
  ws['!cols'] = EXCEL_COLUMNS.map(() => ({ wch: 22 }));
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'Students');
  XLSX.writeFile(wb, 'student_import_template.xlsx');
}

function parseExcelRow(row) {
  // Accepts either the template's friendly headers or plain lowercase field names,
  // so a sheet doesn't have to match exactly to be imported.
  const get = (...keys) => {
    for (const k of keys) {
      if (row[k] !== undefined && row[k] !== null && row[k] !== '') return row[k];
    }
    return '';
  };
  const skillsRaw = get('Skills (comma separated)', 'Skills', 'skills');

  // Any column not in the known set becomes a flexible additionalDetails entry
  // (e.g. "UG CGPA", "PG CGPA", "10th Percentage") — parsed as a number, skipped if not numeric.
  const additionalDetails = {};
  for (const key of Object.keys(row)) {
    if (KNOWN_HEADER_KEYS.has(key.trim().toLowerCase())) continue;
    const num = parseFloat(row[key]);
    if (!Number.isNaN(num)) additionalDetails[key.trim()] = num;
  }

  // Fall back to a recognizable "*CGPA" column (e.g. "UG CGPA") if a plain "CGPA" column is absent.
  let cgpa = parseFloat(get('CGPA', 'cgpa'));
  if (Number.isNaN(cgpa)) {
    const cgpaLikeKey = Object.keys(row).find((k) => /cgpa/i.test(k));
    if (cgpaLikeKey) cgpa = parseFloat(row[cgpaLikeKey]) || 0;
    else cgpa = 0;
  }

  return {
    name: String(get('Name', 'name')).trim(),
    email: String(get('Email', 'email')).trim(),
    rollNumber: String(get('Roll Number', 'rollNumber', 'roll number')).trim(),
    department: String(get('Department', 'department')).trim(),
    degree: String(get('Degree', 'degree')).trim() || 'MCA',
    cgpa,
    backlogs: parseInt(get('Backlogs', 'backlogs')) || 0,
    graduationYear: parseInt(get('Graduation Year', 'graduationYear', 'graduation year')) || new Date().getFullYear(),
    skills: String(skillsRaw || '').split(',').map((s) => s.trim()).filter(Boolean),
    additionalDetails
  };
}

export default function Students() {
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [importing, setImporting] = useState(false);
  const fileInputRef = useRef(null);
  const { push, errorFromException } = useToast();

  // Dynamic filter criteria for bulk selection
  const [deptFilter, setDeptFilter] = useState('');
  const [cgpaOp, setCgpaOp] = useState('>='); // >=, <=, ==
  const [cgpaVal, setCgpaVal] = useState('');
  const [maxBacklogs, setMaxBacklogs] = useState('');
  const [selected, setSelected] = useState(new Set());
  const [academicOptions, setAcademicOptions] = useState({ departments: [], degrees: [] });

  useEffect(() => {
    academicSettingsApi.get().then(({ data }) => setAcademicOptions(data)).catch(() => {});
  }, []);

  const load = async () => {
    const { data } = await studentApi.getAll();
    setStudents(data);
  };

  useEffect(() => { load(); }, []);

  const departments = useMemo(() => [...new Set(students.map((s) => s.department).filter(Boolean))], [students]);

  const filtered = useMemo(() => {
    const q = search.toLowerCase();
    return students.filter((s) => {
      const matchesSearch = s.name?.toLowerCase().includes(q) || s.rollNumber?.toLowerCase().includes(q) || s.department?.toLowerCase().includes(q);
      const matchesDept = !deptFilter || s.department === deptFilter;
      const matchesCgpa = !cgpaVal || (
        cgpaOp === '>=' ? s.cgpa >= parseFloat(cgpaVal) :
        cgpaOp === '<=' ? s.cgpa <= parseFloat(cgpaVal) :
        s.cgpa === parseFloat(cgpaVal)
      );
      const matchesBacklogs = maxBacklogs === '' || s.backlogs <= parseInt(maxBacklogs);
      return matchesSearch && matchesDept && matchesCgpa && matchesBacklogs;
    });
  }, [students, search, deptFilter, cgpaOp, cgpaVal, maxBacklogs]);

  const toggleSelect = (id) => {
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const selectAllFiltered = () => {
    setSelected(new Set(filtered.map((s) => s.id)));
  };

  const clearSelection = () => setSelected(new Set());

  const removeSelected = async () => {
    if (selected.size === 0) return;
    if (!confirm(`Remove ${selected.size} student record(s)? This cannot be undone.`)) return;
    let ok = 0, fail = 0;
    for (const id of selected) {
      try {
        await studentApi.delete(id);
        ok++;
      } catch (err) {
        fail++;
      }
    }
    push(`Removed ${ok} student(s)${fail ? `, ${fail} failed` : ''}.`, fail && !ok ? 'error' : 'success');
    clearSelection();
    load();
  };

  const removeDuplicates = async () => {
    const seen = new Map(); // key -> first student kept
    const duplicateIds = [];
    for (const s of students) {
      const key = (s.email || '').toLowerCase().trim() || `roll:${(s.rollNumber || '').toLowerCase().trim()}`;
      if (seen.has(key)) {
        duplicateIds.push(s.id);
      } else {
        seen.set(key, s.id);
      }
    }
    if (duplicateIds.length === 0) {
      push('No duplicate students found (matched by email/roll number).', 'success');
      return;
    }
    if (!confirm(`Found ${duplicateIds.length} duplicate record(s) (same email or roll number as an earlier row). Remove them, keeping the first occurrence of each?`)) return;
    let ok = 0, fail = 0;
    for (const id of duplicateIds) {
      try {
        await studentApi.delete(id);
        ok++;
      } catch (err) {
        fail++;
      }
    }
    push(`Removed ${ok} duplicate(s)${fail ? `, ${fail} failed` : ''}.`, 'success');
    load();
  };

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const openAdd = () => {
    setEditingId(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (s) => {
    setEditingId(s.id);
    setForm({
      name: s.name || '', email: s.email || '', rollNumber: s.rollNumber || '',
      department: s.department || '', degree: s.degree || '',
      cgpa: s.cgpa ?? '', backlogs: s.backlogs ?? 0, graduationYear: s.graduationYear || 2027,
      skills: (s.skills || []).join(', '),
      detailRows: Object.entries(s.additionalDetails || {}).map(([key, value]) => ({ key, value: String(value) }))
    });
    setShowForm(true);
  };

  const addDetailRow = () => setForm((f) => ({ ...f, detailRows: [...f.detailRows, { key: '', value: '' }] }));
  const updateDetailRow = (i, field, val) => setForm((f) => {
    const rows = [...f.detailRows];
    rows[i] = { ...rows[i], [field]: val };
    return { ...f, detailRows: rows };
  });
  const removeDetailRow = (i) => setForm((f) => ({ ...f, detailRows: f.detailRows.filter((_, idx) => idx !== i) }));

  const submit = async (e) => {
    e.preventDefault();
    const additionalDetails = {};
    for (const row of form.detailRows) {
      if (row.key.trim() && row.value !== '' && !Number.isNaN(parseFloat(row.value))) {
        additionalDetails[row.key.trim()] = parseFloat(row.value);
      }
    }
    const payload = {
      name: form.name, email: form.email, rollNumber: form.rollNumber,
      department: form.department, degree: form.degree,
      cgpa: parseFloat(form.cgpa) || 0,
      backlogs: parseInt(form.backlogs) || 0,
      graduationYear: parseInt(form.graduationYear),
      skills: form.skills.split(',').map((s) => s.trim()).filter(Boolean),
      additionalDetails
    };
    try {
      if (editingId) {
        await studentApi.update(editingId, payload);
        push('Student updated.', 'success');
      } else {
        await studentApi.create(payload);
        push('Student added.', 'success');
      }
      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      errorFromException(err, editingId ? 'Could not update student.' : 'Could not add student.');
    }
  };

  const remove = async (id) => {
    if (!confirm('Remove this student record?')) return;
    try {
      await studentApi.delete(id);
      push('Student removed.', 'success');
      load();
    } catch (err) {
      errorFromException(err);
    }
  };

  const resetPassword = async (student) => {
    if (!student.userId) {
      push("This student doesn't have a login account yet.", 'error');
      return;
    }
    const newPassword = prompt(`Set a new password for ${student.name} (min 6 characters):`);
    if (!newPassword) return;
    if (newPassword.length < 6) {
      push('Password must be at least 6 characters.', 'error');
      return;
    }
    try {
      await adminUserApi.resetPassword(student.userId, newPassword);
      push(`Password reset for ${student.name}. Share the new password with them securely.`, 'success');
    } catch (err) {
      errorFromException(err);
    }
  };

  const handleImportFile = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    try {
      const buffer = await file.arrayBuffer();
      const wb = XLSX.read(buffer, { type: 'array' });
      const sheet = wb.Sheets[wb.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json(sheet, { defval: '' });

      let created = 0;
      let failed = 0;
      for (const row of rows) {
        const payload = parseExcelRow(row);
        if (!payload.name || !payload.email || !payload.rollNumber) {
          failed++;
          continue;
        }
        try {
          await studentApi.create(payload);
          created++;
        } catch (err) {
          failed++;
        }
      }
      push(`Import finished: ${created} added${failed ? `, ${failed} skipped (missing fields or duplicate)` : ''}.`, failed && !created ? 'error' : 'success');
      load();
    } catch (err) {
      push('Could not read that file. Make sure it is a valid .xlsx file.', 'error');
    } finally {
      setImporting(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  return (
    <div>
      <div className="section-title">
        <h1>Students</h1>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-outline" onClick={downloadTemplate}>Download Excel Template</button>
          <button className="btn btn-outline" disabled={importing} onClick={() => fileInputRef.current?.click()}>
            {importing ? 'Importing…' : 'Import from Excel'}
          </button>
          <input ref={fileInputRef} type="file" accept=".xlsx,.xls" style={{ display: 'none' }} onChange={handleImportFile} />
          <button className="btn btn-amber" onClick={openAdd}>+ Add Student</button>
        </div>
      </div>
      <div className="form-group" style={{ maxWidth: 320 }}>
        <input placeholder="Search by name, roll no, department…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      <div className="card" style={{ marginBottom: 16, display: 'flex', gap: 14, alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label>Department</label>
          <select value={deptFilter} onChange={(e) => setDeptFilter(e.target.value)}>
            <option value="">Any</option>
            {departments.map((d) => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label>CGPA</label>
          <div style={{ display: 'flex', gap: 6 }}>
            <select value={cgpaOp} onChange={(e) => setCgpaOp(e.target.value)}>
              <option value=">=">≥</option>
              <option value="<=">≤</option>
              <option value="==">=</option>
            </select>
            <input type="number" step="0.1" style={{ width: 90 }} placeholder="e.g. 7" value={cgpaVal} onChange={(e) => setCgpaVal(e.target.value)} />
          </div>
        </div>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label>Max backlogs</label>
          <input type="number" style={{ width: 90 }} placeholder="Any" value={maxBacklogs} onChange={(e) => setMaxBacklogs(e.target.value)} />
        </div>
        <button className="btn btn-outline" onClick={selectAllFiltered}>Select all filtered ({filtered.length})</button>
        <button className="btn btn-outline" onClick={removeDuplicates}>Remove Duplicates</button>
        {selected.size > 0 && (
          <>
            <button className="btn btn-outline" onClick={clearSelection}>Clear selection</button>
            <button className="btn btn-danger" onClick={removeSelected}>Remove selected ({selected.size})</button>
          </>
        )}
      </div>

      <div className="card">
        <table>
          <thead>
            <tr>
              <th></th><th>Name</th><th>Roll No.</th><th>Department</th><th>Degree</th><th>CGPA</th><th>Backlogs</th><th>Grad. Year</th><th>Skills</th><th></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((s) => (
              <tr key={s.id}>
                <td><input type="checkbox" checked={selected.has(s.id)} onChange={() => toggleSelect(s.id)} /></td>
                <td>{s.name}</td>
                <td>{s.rollNumber}</td>
                <td>{s.department}</td>
                <td>{s.degree}</td>
                <td>{s.cgpa}</td>
                <td>{s.backlogs}</td>
                <td>{s.graduationYear}</td>
                <td>{(s.skills || []).join(', ')}</td>
                <td style={{ display: 'flex', gap: 6 }}>
                  <button className="btn btn-outline btn-sm" onClick={() => openEdit(s)}>Edit</button>
                  <button className="btn btn-outline btn-sm" onClick={() => resetPassword(s)}>Reset Password</button>
                  <button className="btn btn-danger btn-sm" onClick={() => remove(s.id)}>Remove</button>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && <tr><td colSpan={10} className="empty-state">No students found.</td></tr>}
          </tbody>
        </table>
      </div>

      {showForm && (
        <div className="modal-backdrop" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editingId ? 'Edit Student' : 'Add Student'}</h3>
            <form onSubmit={submit}>
              <div className="form-group"><label>Name</label><input value={form.name} onChange={update('name')} required /></div>
              <div className="form-group"><label>Email</label><input type="email" value={form.email} onChange={update('email')} required /></div>
              <div className="form-row">
                <div className="form-group"><label>Roll number</label><input value={form.rollNumber} onChange={update('rollNumber')} required /></div>
                <div className="form-group">
                  <label>Degree</label>
                  <select value={form.degree} onChange={update('degree')} required>
                    <option value="" disabled>Select…</option>
                    {academicOptions.degrees.map((d) => <option key={d} value={d}>{d}</option>)}
                  </select>
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Department</label>
                  <select value={form.department} onChange={update('department')} required>
                    <option value="" disabled>Select…</option>
                    {academicOptions.departments.map((d) => <option key={d} value={d}>{d}</option>)}
                  </select>
                </div>
                <div className="form-group"><label>Graduation year</label><input type="number" value={form.graduationYear} onChange={update('graduationYear')} required /></div>
              </div>
              <div className="form-row">
                <div className="form-group"><label>CGPA</label><input type="number" step="0.1" value={form.cgpa} onChange={update('cgpa')} required /></div>
                <div className="form-group"><label>Backlogs</label><input type="number" value={form.backlogs} onChange={update('backlogs')} required /></div>
              </div>
              <div className="form-group"><label>Skills (comma separated)</label><input value={form.skills} onChange={update('skills')} placeholder="Java, Spring Boot, MongoDB" /></div>

              <div className="form-group">
                <label>Additional details (optional — 10th %, 12th %, UG CGPA, PG CGPA, etc.)</label>
                <p className="muted" style={{ marginBottom: 8 }}>Add any extra numeric detail a company might ask for. The label you type here is what drives will match against.</p>
                {form.detailRows.map((row, i) => (
                  <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                    <input placeholder="e.g. 10th Percentage" value={row.key} onChange={(e) => updateDetailRow(i, 'key', e.target.value)} style={{ flex: 2 }} />
                    <input type="number" step="0.1" placeholder="Value" value={row.value} onChange={(e) => updateDetailRow(i, 'value', e.target.value)} style={{ flex: 1 }} />
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => removeDetailRow(i)}>✕</button>
                  </div>
                ))}
                <button type="button" className="btn btn-outline btn-sm" onClick={addDetailRow}>+ Add detail</button>
              </div>

              <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
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
