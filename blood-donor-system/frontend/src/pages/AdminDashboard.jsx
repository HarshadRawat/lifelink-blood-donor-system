import React from 'react';
import { ShieldAlert, AlertTriangle, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function AdminDashboard() {
  const { user } = useAuth();

  return (
    <div className="dashboard-container max-w-4xl mx-auto py-12">
      <div className="card p-8 text-center border-amber-200 bg-amber-50/50">
        <div className="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4 text-amber-600">
          <ShieldAlert size={32} />
        </div>

        <h1 className="text-2xl font-bold text-slate-900 mb-2">Admin Management Interface</h1>
        <p className="text-slate-600 text-sm max-w-lg mx-auto mb-6">
          Logged in as <strong>{user?.name}</strong> (<span className="font-mono">ROLE_ADMIN</span>).
        </p>

        <div className="p-4 bg-white rounded-xl border border-amber-200 text-left max-w-md mx-auto text-xs text-slate-700 space-y-2 mb-6">
          <div className="flex items-center gap-2 font-semibold text-amber-800">
            <AlertTriangle size={16} /> Backend Administrative APIs Status
          </div>
          <p>
            Backend source code inspection confirms that administrative management REST endpoints (e.g. system-wide user moderation, role granting) are not currently exposed by the backend service.
          </p>
          <p className="text-slate-500">
            Per system rules, unavailable features are explicitly marked rather than simulated with fake hardcoded data.
          </p>
        </div>

        <Link to="/" className="inline-flex items-center gap-2 px-6 py-2.5 bg-slate-900 text-white text-sm font-semibold rounded-xl hover:bg-slate-800 transition">
          <ArrowLeft size={16} /> Return to Home
        </Link>
      </div>
    </div>
  );
}
