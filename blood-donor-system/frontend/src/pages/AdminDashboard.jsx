import React, { useState, useEffect, useCallback } from 'react';
import {
  ShieldAlert,
  Building2,
  Users,
  Activity,
  Award,
  RefreshCw,
  Heart,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  MapPin,
  Calendar,
  Layers,
  CheckCircle2,
  Phone
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { UrgencyBadge, RequestStatusBadge } from '../components/Badges';

export function AdminDashboard() {
  const { user } = useAuth();
  const { showToast } = useNotifications();

  // Metrics State
  const [metrics, setMetrics] = useState({
    totalUsers: 0,
    totalDonors: 0,
    totalHospitals: 0,
    totalEmergencyRequests: 0,
    activeEmergencyRequests: 0,
    fulfilledEmergencyRequests: 0,
    completedDonations: 0,
  });
  const [metricsLoading, setMetricsLoading] = useState(true);

  // Active Tab: 'requests' | 'users' | 'hospitals'
  const [activeTab, setActiveTab] = useState('requests');

  // Emergency Requests Table State
  const [requests, setRequests] = useState([]);
  const [requestsLoading, setRequestsLoading] = useState(true);
  const [requestsPage, setRequestsPage] = useState(0);
  const [requestsTotalPages, setRequestsTotalPages] = useState(0);
  const [requestsTotalElements, setRequestsTotalElements] = useState(0);

  // Users Table State
  const [usersList, setUsersList] = useState([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [usersPage, setUsersPage] = useState(0);
  const [usersTotalPages, setUsersTotalPages] = useState(0);
  const [usersTotalElements, setUsersTotalElements] = useState(0);

  // Pending Hospitals Table State
  const [pendingHospitals, setPendingHospitals] = useState([]);
  const [pendingLoading, setPendingLoading] = useState(true);
  const [pendingPage, setPendingPage] = useState(0);
  const [pendingTotalPages, setPendingTotalPages] = useState(0);
  const [pendingTotalElements, setPendingTotalElements] = useState(0);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  // Fetch Metrics from DB
  const fetchMetrics = useCallback(async () => {
    try {
      setMetricsLoading(true);
      const data = await api.admin.getMetrics();
      if (data) {
        setMetrics(data);
      }
    } catch (err) {
      console.error('Failed to load admin metrics:', err);
    } finally {
      setMetricsLoading(false);
    }
  }, []);

  // Fetch Live Emergency Requests from DB
  const fetchRequests = useCallback(async (page = 0) => {
    try {
      setRequestsLoading(true);
      const response = await api.admin.getRecentRequests({ page, size: 8 });
      if (response && response.content) {
        setRequests(response.content);
        setRequestsTotalPages(response.totalPages || 0);
        setRequestsTotalElements(response.totalElements || 0);
        setRequestsPage(page);
      }
    } catch (err) {
      console.error('Failed to load emergency requests:', err);
    } finally {
      setRequestsLoading(false);
    }
  }, []);

  // Fetch Registered Users from DB
  const fetchUsers = useCallback(async (page = 0) => {
    try {
      setUsersLoading(true);
      const response = await api.admin.getUsers({ page, size: 8 });
      if (response && response.content) {
        setUsersList(response.content);
        setUsersTotalPages(response.totalPages || 0);
        setUsersTotalElements(response.totalElements || 0);
        setUsersPage(page);
      }
    } catch (err) {
      console.error('Failed to load users:', err);
    } finally {
      setUsersLoading(false);
    }
  }, []);

  // Fetch Pending Hospitals from DB
  const fetchPendingHospitals = useCallback(async (page = 0) => {
    try {
      setPendingLoading(true);
      const response = await api.admin.getPendingHospitals({ page, size: 8 });
      if (response && response.content) {
        setPendingHospitals(response.content);
        setPendingTotalPages(response.totalPages || 0);
        setPendingTotalElements(response.totalElements || 0);
        setPendingPage(page);
      }
    } catch (err) {
      console.error('Failed to load pending hospitals:', err);
    } finally {
      setPendingLoading(false);
    }
  }, []);

  // Initial Data Fetch
  useEffect(() => {
    fetchMetrics();
    fetchRequests(0);
    fetchUsers(0);
    fetchPendingHospitals(0);
  }, [fetchMetrics, fetchRequests, fetchUsers, fetchPendingHospitals]);

  const handleRefresh = () => {
    fetchMetrics();
    fetchPendingHospitals(0);
    if (activeTab === 'requests') {
      fetchRequests(requestsPage);
    } else if (activeTab === 'users') {
      fetchUsers(usersPage);
    } else {
      fetchPendingHospitals(pendingPage);
    }
  };

  const handleVerifyHospital = async (hospitalId, hospitalName) => {
    try {
      setActionLoadingId(hospitalId);
      await api.admin.verifyHospital(hospitalId, true);
      showToast(
        'Hospital Approved',
        `${hospitalName} has been verified and can now broadcast emergency blood requests.`,
        'success'
      );
      fetchPendingHospitals(pendingPage);
      fetchMetrics();
    } catch (err) {
      showToast('Verification Failed', err.message || 'Could not verify hospital.', 'error');
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="admin-portal">
      {/* 1. HERO BANNER */}
      <div className="admin-hero">
        <div className="admin-hero-left">
          <div className="admin-hero-icon">
            <ShieldCheck size={28} />
          </div>
          <div>
            <div className="admin-hero-title-row">
              <h1 className="admin-hero-title">System Oversight & Admin Portal</h1>
              <span className="admin-role-badge">ROLE_ADMIN</span>
            </div>
            <p className="admin-hero-sub">
              Logged in as <strong>{user?.name || 'LifeLink System Admin'}</strong> ({user?.email || 'admin@lifelink.com'})
            </p>
          </div>
        </div>

        <button
          onClick={handleRefresh}
          disabled={metricsLoading}
          className="admin-hero-refresh-btn"
          title="Refresh live database data"
        >
          <RefreshCw size={15} className={metricsLoading ? 'animate-spin' : ''} />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* Real-time Emergency Operations Center Banner */}
      <div className="mb-6 p-4 rounded-2xl bg-gradient-to-r from-slate-900 via-red-950 to-slate-900 text-white shadow-md border border-red-900/40 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="relative flex items-center justify-center w-10 h-10 rounded-xl bg-red-600/30 border border-red-500/40 text-red-400">
            <ShieldCheck size={22} className="animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="inline-block w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
              <h3 className="font-bold text-sm text-white tracking-wide uppercase">LifeLink Operations Center Active</h3>
            </div>
            <p className="text-xs text-slate-300 mt-0.5">
              Platform High Availability • Real-time HIPAA/GDPR Compliance & Institutional Governance.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 text-xs font-semibold">
          <span className="px-3 py-1.5 rounded-lg bg-white/10 border border-white/15 text-slate-200">
            Platform Users: <strong className="text-white">{metrics.totalUsers}</strong>
          </span>
          <span className="px-3 py-1.5 rounded-lg bg-amber-500/20 border border-amber-500/30 text-amber-200">
            Pending Approvals: <strong className="text-white">{pendingTotalElements}</strong>
          </span>
        </div>
      </div>

      {/* 2. FOUR-COLUMN KPI METRICS GRID */}
      <div className="admin-stats-grid">
        {/* Card 1: Registered Donors */}
        <div className="admin-stat-card card-donors">
          <div className="admin-stat-top">
            <span className="admin-stat-label">Registered Donors</span>
            <div className="admin-stat-icon-box rose">
              <Heart size={20} />
            </div>
          </div>
          <div className="admin-stat-value">
            {metricsLoading ? '—' : metrics.totalDonors}
          </div>
          <p className="admin-stat-sub">
            Out of {metrics.totalUsers} total registered accounts
          </p>
        </div>

        {/* Card 2: Registered Hospitals */}
        <div className="admin-stat-card card-hospitals">
          <div className="admin-stat-top">
            <span className="admin-stat-label">Registered Hospitals</span>
            <div className="admin-stat-icon-box blue">
              <Building2 size={20} />
            </div>
          </div>
          <div className="admin-stat-value">
            {metricsLoading ? '—' : metrics.totalHospitals}
          </div>
          <p className="admin-stat-sub">
            Verified healthcare institutions
          </p>
        </div>

        {/* Card 3: Active Emergency Requests */}
        <div className="admin-stat-card card-active">
          <div className="admin-stat-top">
            <span className="admin-stat-label">Active Emergencies</span>
            <div className="admin-stat-icon-box amber">
              <Activity size={20} />
            </div>
          </div>
          <div className="admin-stat-value amber">
            {metricsLoading ? '—' : metrics.activeEmergencyRequests}
          </div>
          <p className="admin-stat-sub">
            {metrics.totalEmergencyRequests} lifetime emergency tickets
          </p>
        </div>

        {/* Card 4: Fulfilled Blood Donations */}
        <div className="admin-stat-card card-fulfilled">
          <div className="admin-stat-top">
            <span className="admin-stat-label">Fulfilled Donations</span>
            <div className="admin-stat-icon-box emerald">
              <Award size={20} />
            </div>
          </div>
          <div className="admin-stat-value emerald">
            {metricsLoading ? '—' : metrics.fulfilledEmergencyRequests}
          </div>
          <p className="admin-stat-sub">
            {metrics.completedDonations} confirmed match handoffs
          </p>
        </div>
      </div>

      {/* 3. TABS NAVIGATION */}
      <div className="admin-tabs">
        <button
          onClick={() => setActiveTab('requests')}
          className={`admin-tab-btn ${activeTab === 'requests' ? 'active' : ''}`}
        >
          <Activity size={16} />
          <span>Emergency Requests Monitor</span>
          <span className="admin-tab-count">{requestsTotalElements}</span>
        </button>

        <button
          onClick={() => setActiveTab('users')}
          className={`admin-tab-btn ${activeTab === 'users' ? 'active' : ''}`}
        >
          <Users size={16} />
          <span>Registered Users Directory</span>
          <span className="admin-tab-count">{usersTotalElements}</span>
        </button>

        <button
          onClick={() => setActiveTab('hospitals')}
          className={`admin-tab-btn ${activeTab === 'hospitals' ? 'active' : ''}`}
        >
          <Building2 size={16} />
          <span>Pending Hospital Approvals</span>
          <span className={`admin-tab-count ${pendingTotalElements > 0 ? 'bg-amber-100 text-amber-800 font-bold' : ''}`}>
            {pendingTotalElements}
          </span>
        </button>
      </div>

      {/* 4. TAB CONTENT PANELS */}
      {/* TAB 1: Live Emergency Requests */}
      {activeTab === 'requests' && (
        <div className="admin-panel-card">
          <div className="admin-panel-header">
            <h2 className="admin-panel-title">System Emergency Requests</h2>
            <p className="admin-panel-sub">
              Live database records of blood emergency requests across all healthcare institutions
            </p>
          </div>

          {requestsLoading ? (
            <div style={{ textAlign: 'center', padding: '3.5rem 0', color: '#64748b' }}>
              <RefreshCw className="animate-spin" size={26} style={{ color: '#dc2626', marginBottom: '0.75rem' }} />
              <p style={{ margin: 0, fontSize: '0.9rem' }}>Loading emergency requests from database...</p>
            </div>
          ) : requests.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '3.5rem 0', color: '#94a3b8' }}>
              <Activity size={40} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
              <p style={{ fontWeight: 600, fontSize: '0.95rem', margin: 0 }}>No emergency requests recorded in database yet</p>
            </div>
          ) : (
            <div className="admin-table-wrapper">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Ticket</th>
                    <th>Hospital</th>
                    <th>Blood Group</th>
                    <th>Units</th>
                    <th>Urgency</th>
                    <th>Status</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((req) => (
                    <tr key={req.id}>
                      <td>
                        <span className="admin-ticket-id">#{req.id}</span>
                      </td>
                      <td>
                        <div style={{ fontWeight: 700, color: '#0f172a' }}>{req.hospitalName}</div>
                        <div style={{ fontSize: '0.75rem', color: '#64748b', display: 'flex', alignItems: 'center', gap: '3px' }}>
                          <MapPin size={11} /> {req.hospitalCity}
                        </div>
                      </td>
                      <td>
                        <span className="admin-blood-badge">{req.bloodGroup}</span>
                      </td>
                      <td style={{ fontWeight: 600, color: '#334155' }}>
                        {req.unitsRequired} {req.unitsRequired === 1 ? 'unit' : 'units'}
                      </td>
                      <td>
                        <UrgencyBadge urgency={req.urgency} />
                      </td>
                      <td>
                        <RequestStatusBadge status={req.status} />
                      </td>
                      <td style={{ fontSize: '0.8rem', color: '#64748b', whiteSpace: 'nowrap' }}>
                        {new Date(req.createdAt).toLocaleDateString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Pagination */}
              {requestsTotalPages > 1 && (
                <div className="admin-pagination-bar">
                  <span>Page {requestsPage + 1} of {requestsTotalPages} ({requestsTotalElements} total requests)</span>
                  <div className="admin-page-btn-group">
                    <button
                      onClick={() => fetchRequests(requestsPage - 1)}
                      disabled={requestsPage === 0}
                      className="admin-page-btn"
                    >
                      <ChevronLeft size={14} /> Previous
                    </button>
                    <button
                      onClick={() => fetchRequests(requestsPage + 1)}
                      disabled={requestsPage >= requestsTotalPages - 1}
                      className="admin-page-btn"
                    >
                      Next <ChevronRight size={14} />
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: Users Directory */}
      {activeTab === 'users' && (
        <div className="admin-panel-card">
          <div className="admin-panel-header">
            <h2 className="admin-panel-title">Platform Accounts Directory</h2>
            <p className="admin-panel-sub">
              Live database records of registered donors, healthcare institutions, and administrative accounts
            </p>
          </div>

          {usersLoading ? (
            <div style={{ textAlign: 'center', padding: '3.5rem 0', color: '#64748b' }}>
              <RefreshCw className="animate-spin" size={26} style={{ color: '#dc2626', marginBottom: '0.75rem' }} />
              <p style={{ margin: 0, fontSize: '0.9rem' }}>Loading user directory from database...</p>
            </div>
          ) : usersList.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '3.5rem 0', color: '#94a3b8' }}>
              <Users size={40} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
              <p style={{ fontWeight: 600, fontSize: '0.95rem', margin: 0 }}>No registered users found</p>
            </div>
          ) : (
            <div className="admin-table-wrapper">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>User ID</th>
                    <th>Full Name</th>
                    <th>Email Address</th>
                    <th>Assigned Role</th>
                  </tr>
                </thead>
                <tbody>
                  {usersList.map((u) => {
                    const roleClass = u.role ? u.role.toLowerCase() : 'donor';
                    return (
                      <tr key={u.id}>
                        <td>
                          <span className="admin-ticket-id">#{u.id}</span>
                        </td>
                        <td style={{ fontWeight: 700, color: '#0f172a' }}>
                          {u.name}
                        </td>
                        <td style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: '#475569' }}>
                          {u.email}
                        </td>
                        <td>
                          <span className={`admin-role-pill ${roleClass}`}>
                            {u.role}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {/* Pagination */}
              {usersTotalPages > 1 && (
                <div className="admin-pagination-bar">
                  <span>Page {usersPage + 1} of {usersTotalPages} ({usersTotalElements} total accounts)</span>
                  <div className="admin-page-btn-group">
                    <button
                      onClick={() => fetchUsers(usersPage - 1)}
                      disabled={usersPage === 0}
                      className="admin-page-btn"
                    >
                      <ChevronLeft size={14} /> Previous
                    </button>
                    <button
                      onClick={() => fetchUsers(usersPage + 1)}
                      disabled={usersPage >= usersTotalPages - 1}
                      className="admin-page-btn"
                    >
                      Next <ChevronRight size={14} />
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: Pending Hospital Approvals */}
      {activeTab === 'hospitals' && (
        <div className="admin-panel-card">
          <div className="admin-panel-header">
            <h2 className="admin-panel-title">Pending Hospital Approvals</h2>
            <p className="admin-panel-sub">
              Review healthcare facility credentials and grant emergency broadcast authorization
            </p>
          </div>

          {pendingLoading ? (
            <div style={{ textAlign: 'center', padding: '3.5rem 0', color: '#64748b' }}>
              <RefreshCw className="animate-spin" size={26} style={{ color: '#dc2626', marginBottom: '0.75rem' }} />
              <p style={{ margin: 0, fontSize: '0.9rem' }}>Loading pending hospitals from database...</p>
            </div>
          ) : pendingHospitals.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '3.5rem 0', color: '#94a3b8' }}>
              <CheckCircle2 size={44} style={{ margin: '0 auto 0.75rem', color: '#10b981' }} />
              <p style={{ fontWeight: 700, fontSize: '1rem', color: '#0f172a', margin: '0 0 0.25rem' }}>All Facilities Verified</p>
              <p style={{ fontSize: '0.875rem', color: '#64748b', margin: 0 }}>There are no healthcare facilities currently awaiting approval.</p>
            </div>
          ) : (
            <div className="admin-table-wrapper">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Facility ID</th>
                    <th>Hospital Name</th>
                    <th>License / Reg No</th>
                    <th>Address & City</th>
                    <th>Contact Phone</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingHospitals.map((h) => (
                    <tr key={h.id}>
                      <td>
                        <span className="admin-ticket-id">#{h.id}</span>
                      </td>
                      <td style={{ fontWeight: 700, color: '#0f172a' }}>
                        {h.hospitalName}
                      </td>
                      <td style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: '#334155' }}>
                        {h.registrationNumber}
                      </td>
                      <td>
                        <div style={{ fontSize: '0.85rem', color: '#334155' }}>{h.address}</div>
                        <div style={{ fontSize: '0.75rem', color: '#64748b', display: 'flex', alignItems: 'center', gap: '3px' }}>
                          <MapPin size={11} /> {h.city}
                        </div>
                      </td>
                      <td style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: '#475569' }}>
                        {h.contactNumber}
                      </td>
                      <td>
                        <button
                          onClick={() => handleVerifyHospital(h.id, h.hospitalName)}
                          disabled={actionLoadingId === h.id}
                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold inline-flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
                        >
                          <ShieldCheck size={14} />
                          <span>{actionLoadingId === h.id ? 'Approving...' : 'Approve & Verify'}</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Pagination */}
              {pendingTotalPages > 1 && (
                <div className="admin-pagination-bar">
                  <span>Page {pendingPage + 1} of {pendingTotalPages} ({pendingTotalElements} pending)</span>
                  <div className="admin-page-btn-group">
                    <button
                      onClick={() => fetchPendingHospitals(pendingPage - 1)}
                      disabled={pendingPage === 0}
                      className="admin-page-btn"
                    >
                      <ChevronLeft size={14} /> Previous
                    </button>
                    <button
                      onClick={() => fetchPendingHospitals(pendingPage + 1)}
                      disabled={pendingPage >= pendingTotalPages - 1}
                      className="admin-page-btn"
                    >
                      Next <ChevronRight size={14} />
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
