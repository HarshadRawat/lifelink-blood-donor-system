import React from 'react';
import { X, Bell, Check, AlertCircle, Building2, Droplet, ArrowRight, CheckCircle2, UserX } from 'lucide-react';
import { useNotifications } from '../context/NotificationContext';
import { useAuth } from '../context/AuthContext';
import { getRequestDetails } from '../utils/requestRegistry';

export function NotificationDrawer({ isOpen, onClose, onSelectRequest }) {
  const { notifications, unreadCount, markAsRead, loading } = useNotifications();
  const { role } = useAuth();

  if (!isOpen) return null;

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch (e) {
      return dateStr;
    }
  };

  const formatMessageWithContext = (item, details) => {
    if (!item.emergencyRequestId) return item.message;

    // For Hospital Notifications: make sure Request #ID is clearly specified
    if (role === 'HOSPITAL' || item.type === 'MATCH_ACCEPTED' || item.type === 'MATCH_REJECTED' || item.type === 'REQUEST_FULFILLED') {
      if (item.message && !item.message.includes(`Request #${item.emergencyRequestId}`)) {
        return item.message.replace(
          /emergency blood request for/i,
          `emergency blood request (Request #${item.emergencyRequestId}) for`
        );
      }
    }

    // For Donor Notifications: include hospital name
    if (role === 'DONOR' || item.type === 'MATCH_FOUND') {
      const hosp = details?.hospitalName || 'Emergency Center';
      if (item.message && !item.message.includes(hosp)) {
        return item.message.replace(
          /emergency blood request for/i,
          `emergency blood request from ${hosp} (Request #${item.emergencyRequestId}) for`
        );
      }
    }

    return item.message;
  };

  const handleRequestClick = (requestId) => {
    if (onSelectRequest) {
      onSelectRequest(requestId);
      onClose();
    }
  };

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <aside className="drawer-panel" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div className="flex items-center gap-2">
            <Bell size={20} className="text-red-600" />
            <h3 className="drawer-title">Notifications</h3>
            {unreadCount > 0 && (
              <span className="unread-badge">{unreadCount} new</span>
            )}
          </div>
          <button onClick={onClose} className="drawer-close-btn" aria-label="Close notifications">
            <X size={20} />
          </button>
        </div>

        <div className="drawer-body">
          {loading && notifications.length === 0 ? (
            <div className="loading-spinner-container">
              <div className="spinner" />
              <p>Loading notifications...</p>
            </div>
          ) : notifications.length === 0 ? (
            <div className="empty-state">
              <Bell size={40} className="empty-icon text-slate-300" />
              <p className="empty-title">No notifications yet</p>
              <p className="empty-desc">You will receive updates here when matches or emergency alerts occur.</p>
            </div>
          ) : (
            <div className="notification-list">
              {notifications.map((item) => {
                const details = item.emergencyRequestId ? getRequestDetails(item.emergencyRequestId) : null;
                const formattedMessage = formatMessageWithContext(item, details);
                const isMatchAccepted = item.type === 'MATCH_ACCEPTED' || (item.title && item.title.includes('Accepted'));
                const isMatchRejected = item.type === 'MATCH_REJECTED' || (item.title && item.title.includes('Rejected'));

                return (
                  <div
                    key={item.id}
                    className={`notification-item ${!item.read ? 'unread' : 'read'}`}
                  >
                    <div className="notification-icon-col">
                      {isMatchAccepted ? (
                        <CheckCircle2 className="text-emerald-600" size={18} />
                      ) : isMatchRejected ? (
                        <UserX className="text-amber-600" size={18} />
                      ) : (
                        <AlertCircle className="notification-type-icon" size={18} />
                      )}
                    </div>

                    <div className="notification-content-col">
                      <div className="notification-top-row">
                        <span className="notification-item-title">{item.title || 'Emergency Update'}</span>
                        <span className="notification-time">{formatDate(item.createdAt)}</span>
                      </div>

                      {/* DONOR PERSPECTIVE: Demanding Hospital Info */}
                      {(role === 'DONOR' || item.type === 'MATCH_FOUND') && item.emergencyRequestId && details && (
                        <div className="notif-hospital-info-box">
                          <div className="flex items-center gap-1.5 font-bold text-red-700 text-xs">
                            <Building2 size={13} className="shrink-0" />
                            <span>Demanding Hospital: <span className="text-slate-900 font-extrabold">{details.hospitalName}</span></span>
                          </div>
                          <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-500">
                            {details.city && <span>📍 {details.city}</span>}
                            <span className="bg-red-50 text-red-700 font-mono font-bold px-1.5 py-0.5 rounded border border-red-200">
                              Request #{item.emergencyRequestId}
                            </span>
                            {details.urgency && (
                              <span className="font-semibold text-slate-600">
                                • {details.urgency}
                              </span>
                            )}
                          </div>
                        </div>
                      )}

                      {/* HOSPITAL PERSPECTIVE: Specific Emergency Request Info */}
                      {(role === 'HOSPITAL' || item.type === 'MATCH_ACCEPTED' || item.type === 'MATCH_REJECTED' || item.type === 'REQUEST_FULFILLED') && item.emergencyRequestId && (
                        <div className="notif-hospital-target-box">
                          <div className="flex items-center justify-between text-xs">
                            <div className="flex items-center gap-1.5 font-semibold text-slate-800">
                              <Droplet size={13} className="text-red-600 shrink-0" />
                              <span>Target Request: <strong className="font-mono text-red-600 font-bold">Request #{item.emergencyRequestId}</strong></span>
                            </div>
                            {details?.urgency && (
                              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${details.urgency === 'CRITICAL' ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'}`}>
                                {details.urgency}
                              </span>
                            )}
                          </div>
                        </div>
                      )}

                      <p className="notification-message mt-1">{formattedMessage}</p>

                      <div className="flex items-center justify-between pt-1">
                        {!item.read ? (
                          <button
                            onClick={() => markAsRead(item.id)}
                            className="mark-read-btn"
                          >
                            <Check size={13} /> Mark as read
                          </button>
                        ) : (
                          <span className="text-[11px] text-slate-400">Read</span>
                        )}

                        {role === 'HOSPITAL' && item.emergencyRequestId && onSelectRequest && (
                          <button
                            onClick={() => handleRequestClick(item.emergencyRequestId)}
                            className="text-[11px] font-bold text-red-600 hover:text-red-700 inline-flex items-center gap-1"
                          >
                            View Request #{item.emergencyRequestId} <ArrowRight size={11} />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </aside>
    </div>
  );
}
