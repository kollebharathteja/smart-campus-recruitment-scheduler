import React, { useEffect, useState } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { interviewApi, studentApi, interviewerApi, driveApi, companyApi } from '../services/api';
import { useAuth } from '../context/AuthContext.jsx';

const STATUS_COLORS = {
  SCHEDULED: '#263a68', CONFIRMED: '#263a68', COMPLETED: '#2f7d5f',
  CANCELLED: '#b3402f', RESCHEDULED: '#d68c33', NO_SHOW: '#b3402f', IN_PROGRESS: '#d68c33'
};

export default function InterviewCalendarPage() {
  const { user } = useAuth();
  const [events, setEvents] = useState([]);
  const [selected, setSelected] = useState(null);
  const [drives, setDrives] = useState({});
  const [companies, setCompanies] = useState({});

  useEffect(() => {
    (async () => {
      const { data: allDrives } = await driveApi.getAll();
      const driveMap = {}; allDrives.forEach((d) => { driveMap[d.id] = d; });
      setDrives(driveMap);
      const { data: allCompanies } = await companyApi.getAll();
      const compMap = {}; allCompanies.forEach((c) => { compMap[c.id] = c; });
      setCompanies(compMap);

      let interviews = [];
      if (user.role === 'ADMIN') {
        const { data } = await interviewApi.getAll();
        interviews = data;
      } else if (user.role === 'STUDENT') {
        const { data: me } = await studentApi.getMe();
        const { data } = await interviewApi.getByStudent(me.id);
        interviews = data;
      } else {
        const { data: me } = await interviewerApi.getMe();
        const { data } = await interviewApi.getByInterviewer(me.id);
        interviews = data;
      }

      setEvents(interviews.map((i) => ({
        id: i.id,
        title: (driveMap[i.driveId]?.jobRole || 'Interview'),
        start: `${i.date}T${i.startTime}`,
        end: `${i.date}T${i.endTime}`,
        backgroundColor: STATUS_COLORS[i.status] || '#666',
        borderColor: STATUS_COLORS[i.status] || '#666',
        extendedProps: i
      })));
    })();
  }, []);

  const eventClick = (info) => {
    setSelected(info.event.extendedProps);
  };

  return (
    <div>
      <h1>Interview Calendar</h1>
      <div className="card" style={{ marginTop: 16 }}>
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="dayGridMonth"
          headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay' }}
          events={events}
          eventClick={eventClick}
          height="auto"
        />
      </div>

      {selected && (
        <div className="modal-backdrop" onClick={() => setSelected(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{drives[selected.driveId]?.jobRole}</h3>
            <p className="muted">{companies[drives[selected.driveId]?.companyId]?.name}</p>
            <table style={{ marginTop: 12 }}>
              <tbody>
                <tr><td>Date</td><td>{selected.date}</td></tr>
                <tr><td>Time</td><td>{selected.startTime} – {selected.endTime}</td></tr>
                <tr><td>Status</td><td>{selected.status}</td></tr>
                <tr><td>Venue</td><td>{selected.venue || '—'}</td></tr>
              </tbody>
            </table>
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <button className="btn btn-outline" onClick={() => setSelected(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
