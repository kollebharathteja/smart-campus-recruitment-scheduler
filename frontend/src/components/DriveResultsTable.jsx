import React from 'react';
import StatusBadge from './StatusBadge.jsx';

function markCell(round) {
  if (round.marks == null) return '—';
  const outOf = round.maxMarks != null ? ` / ${round.maxMarks}` : '';
  return `${round.marks}${outOf}${round.qualified === false ? ' ✕' : ''}`;
}

/** Round-by-round marks and the final outcome for a set of students. */
export default function DriveResultsTable({ results, showCompany = false }) {
  const roundNames = [];
  results.forEach((r) => (r.rounds || []).forEach((round) => {
    if (!roundNames.includes(round.roundName)) roundNames.push(round.roundName);
  }));

  return (
    <table>
      <thead>
        <tr>
          <th>Student</th><th>Roll No.</th><th>Department</th>
          {showCompany && <th>Company</th>}
          {roundNames.map((n) => <th key={n}>{n}</th>)}
          <th>Current round</th><th>Outcome</th>
        </tr>
      </thead>
      <tbody>
        {results.map((r) => (
          <tr key={`${r.applicationId}-${r.driveId}`}>
            <td>{r.studentName}</td>
            <td>{r.rollNumber}</td>
            <td>{r.department}</td>
            {showCompany && <td>{r.companyName}{r.jobRole ? ` — ${r.jobRole}` : ''}</td>}
            {roundNames.map((n) => {
              const round = (r.rounds || []).find((x) => x.roundName === n);
              return <td key={n}>{round ? markCell(round) : '—'}</td>;
            })}
            <td>{r.currentRoundName || '—'}</td>
            <td><StatusBadge status={r.status} /></td>
          </tr>
        ))}
        {results.length === 0 && (
          <tr><td colSpan={roundNames.length + (showCompany ? 6 : 5)} className="empty-state">No results yet.</td></tr>
        )}
      </tbody>
    </table>
  );
}
