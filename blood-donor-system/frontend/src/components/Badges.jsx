import React from 'react';

/**
 * Urgency Badge: LOW, MEDIUM, HIGH, CRITICAL
 */
export function UrgencyBadge({ urgency }) {
  const urgencyUpper = (urgency || '').toUpperCase();
  
  let badgeStyle = 'bg-slate-100 text-slate-700 border-slate-200';
  let icon = '•';

  switch (urgencyUpper) {
    case 'CRITICAL':
      badgeStyle = 'badge-critical';
      icon = '🚨';
      break;
    case 'HIGH':
      badgeStyle = 'badge-high';
      icon = '⚠️';
      break;
    case 'MEDIUM':
      badgeStyle = 'badge-medium';
      icon = '⚡';
      break;
    case 'LOW':
      badgeStyle = 'badge-low';
      icon = 'ℹ️';
      break;
    default:
      break;
  }

  return (
    <span className={`badge ${badgeStyle}`}>
      <span className="badge-icon">{icon}</span>
      {urgencyUpper}
    </span>
  );
}

/**
 * Request Status Badge: ACTIVE, FULFILLED, CANCELLED
 */
export function RequestStatusBadge({ status }) {
  const statusUpper = (status || '').toUpperCase();
  let badgeStyle = 'badge-active';

  if (statusUpper === 'FULFILLED') {
    badgeStyle = 'badge-fulfilled';
  } else if (statusUpper === 'CANCELLED') {
    badgeStyle = 'badge-cancelled';
  }

  return (
    <span className={`badge ${badgeStyle}`}>
      <span className="status-dot-inline" />
      {statusUpper}
    </span>
  );
}

/**
 * Match Status Badge: MATCHED, NOTIFIED, ACCEPTED, REJECTED, COMPLETED
 */
export function MatchStatusBadge({ status }) {
  const statusUpper = (status || '').toUpperCase();
  let badgeStyle = 'badge-matched';

  switch (statusUpper) {
    case 'ACCEPTED':
      badgeStyle = 'badge-fulfilled';
      break;
    case 'COMPLETED':
      badgeStyle = 'badge-completed';
      break;
    case 'REJECTED':
      badgeStyle = 'badge-cancelled';
      break;
    case 'NOTIFIED':
      badgeStyle = 'badge-medium';
      break;
    case 'MATCHED':
    default:
      badgeStyle = 'badge-matched';
      break;
  }

  return <span className={`badge ${badgeStyle}`}>{statusUpper}</span>;
}

/**
 * Availability Badge
 */
export function AvailabilityBadge({ available }) {
  return (
    <span className={`badge ${available ? 'badge-fulfilled' : 'badge-cancelled'}`}>
      <span className={`status-dot-inline ${available ? 'bg-emerald-500' : 'bg-slate-400'}`} />
      {available ? 'AVAILABLE TO DONATE' : 'CURRENTLY UNAVAILABLE'}
    </span>
  );
}
