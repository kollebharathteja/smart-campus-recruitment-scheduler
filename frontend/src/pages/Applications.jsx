import React, { useEffect, useState } from 'react';
import { applicationApi, studentApi, driveApi, companyApi } from '../services/api';
import StatusBadge from '../components/StatusBadge.jsx';

export default function Applications() {
  const [applications, setApplications] = useState([]);
  const [drives, setDrives] = useState({});
  const [companies, setCompanies] = useState({});

  useEffect(() => {
    (async () => {
      const { data: me } = await studentApi.getMe();
      const { data: apps } = await applicationApi.getByStudent(me.id);
      setApplications(apps);
      const { data: allDrives } = await driveApi.getAll();
      const driveMap = {};
      allDrives.forEach((d) => { driveMap[d.id] = d; });
      setDrives(driveMap);
      const { data: allCompanies } = await companyApi.getAll();
      const compMap = {};
      allCompanies.forEach((c) => { compMap[c.id] = c; });
      setCompanies(compMap);
    })();
  }, []);

  return (
    <div>
      <h1>My Applications</h1>
      <div className="card" style={{ marginTop: 16 }}>
        <table>
          <thead><tr><th>Company</th><th>Role</th><th>Status</th><th>Applied</th></tr></thead>
          <tbody>
            {applications.map((a) => {
              const drive = drives[a.driveId];
              const company = drive ? companies[drive.companyId] : null;
              return (
                <tr key={a.id}>
                  <td>{company?.name || '—'}</td>
                  <td>{drive?.jobRole || '—'}</td>
                  <td>
                    <StatusBadge status={a.status} />
                    {a.status === 'NOT_ELIGIBLE' && a.eligibilityReasons?.length > 0 && (
                      <ul style={{ marginTop: 6, paddingLeft: 16, fontSize: 12.5, color: 'var(--red)' }}>
                        {a.eligibilityReasons.map((r, i) => <li key={i}>{r}</li>)}
                      </ul>
                    )}
                  </td>
                  <td>{a.appliedAt?.slice(0, 10)}</td>
                </tr>
              );
            })}
            {applications.length === 0 && <tr><td colSpan={4} className="empty-state">You haven't applied to any drives yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
