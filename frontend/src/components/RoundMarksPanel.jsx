import React, { useEffect, useMemo, useRef, useState } from 'react';
import * as XLSX from 'xlsx';
import { roundResultApi } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

const MARKS_COLUMNS = ['Student Name', 'Department', 'Email', 'Roll Number', 'Round Name', 'Marks', 'Remarks'];

function normalize(value) {
  return String(value ?? '').toLowerCase().replace(/[^a-z0-9]/g, '');
}

/**
 * Marks sheet for one round: the admin (or a lecturer, for their own department) enters or
 * uploads marks for everyone standing in the round. On save, the backend qualifies anyone at
 * or above the cut-off into the next round, rejects the rest, and emails each student.
 */
export default function RoundMarksPanel({ round, onSaved }) {
  const [candidates, setCandidates] = useState([]);
  const [marks, setMarks] = useState({}); // studentId -> { marks, remarks }
  const [cutoff, setCutoff] = useState('');
  const [maxMarks, setMaxMarks] = useState('');
  const [saving, setSaving] = useState(false);
  const fileRef = useRef(null);
  const { push, errorFromException } = useToast();

  const load = async () => {
    const { data } = await roundResultApi.candidates(round.id);
    setCandidates(data);
    const next = {};
    data.forEach((c) => {
      next[c.studentId] = { marks: c.marks ?? '', remarks: c.remarks ?? '' };
    });
    setMarks(next);
    const withCutoff = data.find((c) => c.cutoffMarks != null);
    setCutoff(round.cutoffMarks ?? withCutoff?.cutoffMarks ?? '');
    setMaxMarks(round.maxMarks ?? data.find((c) => c.maxMarks != null)?.maxMarks ?? '');
  };

  useEffect(() => { load(); }, [round.id]);

  const pending = useMemo(() => candidates.filter((c) => c.qualified == null).length, [candidates]);

  const setField = (studentId, field, value) => setMarks((prev) => ({
    ...prev,
    [studentId]: { ...prev[studentId], [field]: value }
  }));

  const downloadMarksTemplate = () => {
    const rows = candidates.map((c) => [c.name, c.department, c.email, c.rollNumber, round.roundName, '', '']);
    const ws = XLSX.utils.aoa_to_sheet([MARKS_COLUMNS, ...rows]);
    ws['!cols'] = MARKS_COLUMNS.map(() => ({ wch: 22 }));
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Marks');
    XLSX.writeFile(wb, `${round.roundName.replace(/\s+/g, '_')}_marks.xlsx`);
  };

  // Uploaded rows are matched to the round's candidates by roll number, falling back to email,
  // so a sheet exported from anywhere (or the template above) can be filled in and re-uploaded.
  const importMarksFile = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const wb = XLSX.read(await file.arrayBuffer(), { type: 'array' });
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { defval: '' });
      const byRoll = new Map(candidates.map((c) => [normalize(c.rollNumber), c.studentId]));
      const byEmail = new Map(candidates.map((c) => [normalize(c.email), c.studentId]));

      let matched = 0;
      const next = { ...marks };
      for (const row of rows) {
        const lookup = {};
        Object.keys(row).forEach((k) => { lookup[normalize(k)] = row[k]; });
        const studentId = byRoll.get(normalize(lookup.rollnumber)) || byEmail.get(normalize(lookup.email));
        if (!studentId) continue;
        const value = parseFloat(lookup.marks);
        if (Number.isNaN(value)) continue;
        next[studentId] = { marks: value, remarks: String(lookup.remarks || '') };
        matched++;
      }
      setMarks(next);
      push(`Loaded marks for ${matched} student(s). Review and click Save & Publish.`, matched ? 'success' : 'error');
    } catch (err) {
      push('Could not read that file. Use .xlsx, .xls or .csv.', 'error');
    } finally {
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const save = async () => {
    const entries = candidates
      .map((c) => ({ candidate: c, entry: marks[c.studentId] }))
      .filter(({ entry }) => entry && entry.marks !== '' && !Number.isNaN(parseFloat(entry.marks)))
      .map(({ candidate, entry }) => ({
        studentId: candidate.studentId,
        marks: parseFloat(entry.marks),
        remarks: entry.remarks || null
      }));

    if (entries.length === 0) {
      push('Enter marks for at least one student first.', 'error');
      return;
    }
    if (cutoff === '' || Number.isNaN(parseFloat(cutoff))) {
      push('Set the qualifying cut-off marks for this round.', 'error');
      return;
    }

    setSaving(true);
    try {
      await roundResultApi.uploadMarks(round.id, {
        cutoffMarks: parseFloat(cutoff),
        maxMarks: maxMarks === '' ? null : parseFloat(maxMarks),
        entries
      });
      push(`Marks saved for ${entries.length} student(s). Qualifiers moved to the next round and everyone was emailed.`, 'success');
      await load();
      onSaved?.();
    } catch (err) {
      errorFromException(err, 'Could not save marks.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div className="form-row" style={{ alignItems: 'end' }}>
        <div className="form-group">
          <label>Qualifying cut-off marks</label>
          <input type="number" step="0.5" value={cutoff} onChange={(e) => setCutoff(e.target.value)} placeholder="e.g. 40" />
        </div>
        <div className="form-group">
          <label>Maximum marks</label>
          <input type="number" step="0.5" value={maxMarks} onChange={(e) => setMaxMarks(e.target.value)} placeholder="e.g. 100" />
        </div>
        <div className="form-group">
          <button className="btn btn-outline" type="button" onClick={downloadMarksTemplate}>Download Marks Sheet</button>
        </div>
        <div className="form-group">
          <button className="btn btn-outline" type="button" onClick={() => fileRef.current?.click()}>Upload Marks Sheet</button>
          <input ref={fileRef} type="file" accept=".xlsx,.xls,.csv" style={{ display: 'none' }} onChange={importMarksFile} />
        </div>
        <div className="form-group">
          <button className="btn btn-amber" type="button" disabled={saving} onClick={save}>
            {saving ? 'Saving…' : 'Save & Publish Results'}
          </button>
        </div>
      </div>
      <p className="muted" style={{ fontSize: 13 }}>
        {candidates.length} candidate(s) in this round, {pending} not graded yet. Students at or above the
        cut-off move to the next round; the rest are rejected. Everyone is emailed their result with the
        date, time, room and panel of what comes next.
      </p>

      <table>
        <thead>
          <tr>
            <th>Student</th><th>Roll No.</th><th>Department</th><th>Email</th>
            <th>Marks</th><th>Remarks</th><th>Result</th>
          </tr>
        </thead>
        <tbody>
          {candidates.map((c) => (
            <tr key={c.studentId}>
              <td>{c.name}</td>
              <td>{c.rollNumber}</td>
              <td>{c.department}</td>
              <td>{c.email}</td>
              <td>
                <input
                  type="number"
                  step="0.5"
                  style={{ width: 90 }}
                  value={marks[c.studentId]?.marks ?? ''}
                  onChange={(e) => setField(c.studentId, 'marks', e.target.value)}
                />
              </td>
              <td>
                <input
                  style={{ width: 160 }}
                  value={marks[c.studentId]?.remarks ?? ''}
                  onChange={(e) => setField(c.studentId, 'remarks', e.target.value)}
                />
              </td>
              <td>
                {c.qualified == null
                  ? <span className="muted">Not graded</span>
                  : <span className={`badge badge-${c.qualified ? 'green' : 'red'}`}>{c.qualified ? 'Qualified' : 'Not qualified'}</span>}
              </td>
            </tr>
          ))}
          {candidates.length === 0 && (
            <tr><td colSpan={7} className="empty-state">No students are standing in this round yet. Shortlist candidates first.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
