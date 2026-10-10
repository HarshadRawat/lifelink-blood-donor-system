import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert } from 'lucide-react';

export function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return (
      <div className="forbidden-container">
        <div className="forbidden-card">
          <ShieldAlert className="forbidden-icon text-red-600" size={48} />
          <h2>Access Denied</h2>
          <p>You do not have permission to view this page. This portal requires <strong>{allowedRoles.join(' or ')}</strong> privileges.</p>
          <p className="text-sm text-slate-500 mt-2">Your current account role is: <strong>{role}</strong>.</p>
          <a href="/" className="primary-btn mt-4 inline-block">
            Return to Home
          </a>
        </div>
      </div>
    );
  }

  return children;
}
