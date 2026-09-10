import React from 'react';

const COLORS = {
  ELIGIBLE: 'green', NOT_ELIGIBLE: 'red', APPLIED: 'navy', SHORTLISTED: 'amber',
  IN_PROCESS: 'amber', SELECTED: 'green', REJECTED: 'red', ON_HOLD: 'grey',
  SCHEDULED: 'navy', CONFIRMED: 'navy', COMPLETED: 'green', CANCELLED: 'red',
  RESCHEDULED: 'amber', NO_SHOW: 'red', OPEN: 'green', CLOSED: 'grey',
  DRAFT: 'grey', ACTIVE: 'green', INACTIVE: 'grey'
};

export default function StatusBadge({ status }) {
  const color = COLORS[status] || 'grey';
  return <span className={`badge badge-${color}`}>{status?.replace(/_/g, ' ')}</span>;
}
