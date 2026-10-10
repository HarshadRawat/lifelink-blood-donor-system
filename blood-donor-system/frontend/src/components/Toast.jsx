import React, { useEffect } from 'react';
import { CheckCircle2, AlertTriangle, Info, XCircle, X } from 'lucide-react';
import { useNotifications } from '../context/NotificationContext';

export function Toast() {
  const { toastMessage, clearToast } = useNotifications();

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => {
        clearToast();
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage, clearToast]);

  if (!toastMessage) return null;

  const { title, message, type } = toastMessage;

  const getIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle2 className="toast-icon text-emerald-500" />;
      case 'warning':
        return <AlertTriangle className="toast-icon text-amber-500" />;
      case 'error':
        return <XCircle className="toast-icon text-rose-500" />;
      case 'info':
      default:
        return <Info className="toast-icon text-blue-500" />;
    }
  };

  return (
    <div className={`toast-container toast-${type || 'info'}`}>
      <div className="toast-content">
        {getIcon()}
        <div className="toast-text">
          {title && <h4 className="toast-title">{title}</h4>}
          <p className="toast-message">{message}</p>
        </div>
      </div>
      <button onClick={clearToast} className="toast-close-btn" aria-label="Close notification">
        <X size={16} />
      </button>
    </div>
  );
}
