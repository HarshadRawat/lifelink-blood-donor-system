import React, { useState, useEffect, useCallback } from 'react';
import { 
  Building2, 
  MapPin, 
  Phone, 
  ShieldCheck, 
  AlertCircle, 
  RefreshCw, 
  Users, 
  Clock, 
  ChevronLeft, 
  ChevronRight, 
  Flame,
  Compass,
  LocateFixed,
  Eye,
  Check,
  X,
  Info,
  Mail
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { Modal } from '../components/Modal';
import { UrgencyBadge, RequestStatusBadge, MatchStatusBadge } from '../components/Badges';
import { registerHospitalRequest, registerHospitalRequestsList } from '../utils/requestRegistry';

export function HospitalDashboard() {
  const { user } = useAuth();
  const { showToast } = useNotifications();

  // Hospital Profile State
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [profileForm, setProfileForm] = useState({
    hospitalName: '',
    registrationNumber: '',
    address: '',
    city: '',
    latitude: 0.0,
    longitude: 0.0,
    contactNumber: '',
  });
  const [profileSubmitting, setProfileSubmitting] = useState(false);
  const [profileModalError, setProfileModalError] = useState('');

  // Emergency Requests State
  const [requests, setRequests] = useState([]);
  const [requestsLoading, setRequestsLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(6);

  // Filters State
  const [filterStatus, setFilterStatus] = useState('');
  const [filterBloodGroup, setFilterBloodGroup] = useState('');
  const [filterUrgency, setFilterUrgency] = useState('');

  // Create Emergency Request Modal
  const [isCreateRequestModalOpen, setIsCreateRequestModalOpen] = useState(false);
  const [createRequestForm, setCreateRequestForm] = useState({
    bloodGroup: 'O-',
    unitsRequired: 1,
    urgency: 'HIGH',
  });
  const [requestSubmitting, setRequestSubmitting] = useState(false);
  const [requestModalError, setRequestModalError] = useState('');

  // View / Find Matches Modal State
  const [activeRequestForMatches, setActiveRequestForMatches] = useState(null);
  const [matchedDonors, setMatchedDonors] = useState([]);
  const [matchesModalLoading, setMatchesModalLoading] = useState(false);
  const [isMatchesModalOpen, setIsMatchesModalOpen] = useState(false);

  // Fetch Hospital Emergency Requests
  const fetchRequests = useCallback(async () => {
    setRequestsLoading(true);
    try {
      const params = {
        page: currentPage,
        size: pageSize,
        sort: 'createdAt,desc',
      };
      if (filterStatus) params.status = filterStatus;
      if (filterBloodGroup) params.bloodGroup = filterBloodGroup;
      if (filterUrgency) params.urgency = filterUrgency;

      const pageData = await api.emergency.getMyRequests(params);
      if (pageData && pageData.content) {
        setRequests(pageData.content);
        registerHospitalRequestsList(pageData.content, profile);
        setTotalPages(pageData.totalPages || 0);
        setTotalElements(pageData.totalElements || 0);
      } else if (Array.isArray(pageData)) {
        setRequests(pageData);
        registerHospitalRequestsList(pageData, profile);
        setTotalPages(1);
        setTotalElements(pageData.length);
      } else {
        setRequests([]);
        setTotalPages(0);
        setTotalElements(0);
      }
    } catch (err) {
      console.error('Failed to fetch emergency requests:', err);
      setRequests([]);
    } finally {
      setRequestsLoading(false);
    }
  }, [currentPage, pageSize, filterStatus, filterBloodGroup, filterUrgency]);

  // Fetch Hospital Profile
  const fetchProfile = useCallback(async () => {
    setProfileLoading(true);
    try {
      const data = await api.hospital.getProfile();
      setProfile(data);
      setProfileForm({
        hospitalName: data.hospitalName || '',
        registrationNumber: data.registrationNumber || '',
        address: data.address || '',
        city: data.city || '',
        latitude: data.latitude || 0.0,
        longitude: data.longitude || 0.0,
        contactNumber: data.contactNumber || '',
      });
    } catch (err) {
      // Backend throws exception if hospital profile does not exist yet
      console.info('Hospital profile not set up yet:', err?.message);
      setProfile(null);
      setRequests([]);
    } finally {
      setProfileLoading(false);
      setRequestsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  useEffect(() => {
    if (profile) {
      fetchRequests();
    }
  }, [profile, fetchRequests]);

  const handleOpenCreateRequest = () => {
    if (!profile) {
      showToast('Profile Required', 'Please set up your hospital profile before creating emergency blood requests.', 'warning');
      setIsProfileModalOpen(true);
      return;
    }
    setIsCreateRequestModalOpen(true);
  };

  // Auto-detect browser GPS location
  const handleDetectLocation = () => {
    if (!navigator.geolocation) {
      showToast('Geolocation Not Supported', 'Your browser does not support GPS location.', 'error');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = parseFloat(pos.coords.latitude.toFixed(4));
        const lon = parseFloat(pos.coords.longitude.toFixed(4));
        setProfileForm((prev) => ({
          ...prev,
          latitude: lat,
          longitude: lon,
        }));
        showToast('GPS Location Detected', `Coordinates set to ${lat}, ${lon}`, 'success');
      },
      () => {
        showToast('Location Access Denied', 'Unable to retrieve location. Please check browser permissions or use demo coordinates.', 'warning');
      }
    );
  };

  // Set demo location (Bangalore Metro Area)
  const handleSetDemoLocation = () => {
    setProfileForm((prev) => ({
      ...prev,
      city: prev.city || 'Bangalore',
      latitude: 12.9716,
      longitude: 77.5946,
    }));
    showToast('Demo Location Set', 'Hospital coordinates set to 12.9716, 77.5946. Pair with Donor demo location (12.9750, 77.5990) to test within 0.6 km!', 'info');
  };

  // Handle Save Hospital Profile
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setProfileModalError('');

    if (!profileForm.hospitalName.trim() || !profileForm.registrationNumber.trim() || !profileForm.city.trim() || !profileForm.contactNumber.trim()) {
      setProfileModalError('Please fill in hospital name, registration number, city, and contact number.');
      return;
    }

    const payload = {
      hospitalName: profileForm.hospitalName.trim(),
      registrationNumber: profileForm.registrationNumber.trim(),
      address: profileForm.address.trim(),
      city: profileForm.city.trim(),
      contactNumber: profileForm.contactNumber.trim(),
      latitude: parseFloat(profileForm.latitude) || 0.0,
      longitude: parseFloat(profileForm.longitude) || 0.0,
    };

    setProfileSubmitting(true);
    try {
      let saved;
      if (profile && profile.id) {
        saved = await api.hospital.updateProfile(payload);
        showToast('Hospital Profile Updated', 'Profile updated successfully.', 'success');
      } else {
        saved = await api.hospital.createProfile(payload);
        showToast('Hospital Profile Created', 'Hospital profile registered successfully.', 'success');
      }
      setProfile(saved);
      setIsProfileModalOpen(false);
      fetchRequests();
    } catch (err) {
      setProfileModalError(err.message || 'Failed to save hospital profile.');
    } finally {
      setProfileSubmitting(false);
    }
  };

  // Handle Create Emergency Request
  const handleCreateRequest = async (e) => {
    e.preventDefault();
    setRequestModalError('');

    if (createRequestForm.unitsRequired < 1) {
      setRequestModalError('Units required must be at least 1.');
      return;
    }

    setRequestSubmitting(true);
    try {
      const created = await api.emergency.createRequest(createRequestForm);
      registerHospitalRequest(created, profile);
      showToast('Emergency Request Broadcast', `Created request for ${created.unitsRequired} units of ${created.bloodGroup}`, 'success');
      setIsCreateRequestModalOpen(false);
      fetchRequests();

      // Automatically trigger match search for new request
      handleFindMatches(created.id);
    } catch (err) {
      setRequestModalError(err.message || 'Failed to create emergency request.');
    } finally {
      setRequestSubmitting(false);
    }
  };

  // Handle Update Request Status
  const handleUpdateStatus = async (requestId, newStatus) => {
    try {
      await api.emergency.updateStatus(requestId, newStatus);
      showToast('Status Updated', `Emergency Request #${requestId} status set to ${newStatus}`, 'info');
      fetchRequests();
    } catch (err) {
      showToast('Status Update Failed', err.message || 'Failed to update request status', 'error');
    }
  };

  // Handle Find Matches (Trigger Algorithmic Match)
  const handleFindMatches = async (requestId) => {
    try {
      const matches = await api.emergency.findMatches(requestId);
      setMatchedDonors(matches || []);
      setActiveRequestForMatches(requestId);
      setIsMatchesModalOpen(true);
      showToast(
        'Match Engine Executed',
        `Found ${matches.length} compatible donor match(es) for Request #${requestId}`,
        'success'
      );
    } catch (err) {
      showToast('Matching Error', err.message || 'Failed to find matching donors', 'error');
    }
  };

  // Handle View Existing Matches
  const handleViewMatches = async (requestId) => {
    setMatchesModalLoading(true);
    setActiveRequestForMatches(requestId);
    setIsMatchesModalOpen(true);
    try {
      const matches = await api.emergency.getRequestMatches(requestId);
      setMatchedDonors(matches || []);
    } catch (err) {
      showToast('Error', err.message || 'Failed to load request matches', 'error');
    } finally {
      setMatchesModalLoading(false);
    }
  };

  // Handle Complete Donor Match
  const handleCompleteMatch = async (matchId) => {
    try {
      await api.emergency.completeMatch(matchId);
      showToast('Match Completed', 'Donation marked as completed successfully!', 'success');
      if (activeRequestForMatches) {
        handleViewMatches(activeRequestForMatches);
      }
      fetchRequests();
    } catch (err) {
      showToast('Error', err.message || 'Match cannot be completed.', 'error');
    }
  };

  return (
    <div className="dashboard-container font-sans">
      {/* Header Banner */}
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-title">Hospital Emergency Portal</h1>
          <p className="dashboard-subtitle">
            Welcome, <strong>{profile?.hospitalName || user?.name}</strong>. Create and track emergency blood requests.
          </p>
        </div>

        <div className="flex gap-3">
          <button
            onClick={handleOpenCreateRequest}
            className="btn-create-request"
          >
            <Flame size={18} />
            <span>New Emergency Request</span>
          </button>
          <button onClick={fetchRequests} className="refresh-btn">
            <RefreshCw size={16} />
          </button>
        </div>
      </div>

      {/* Main Grid */}
      <div className="dashboard-grid">
        {/* Left Column: Hospital Profile */}
        <div className="dashboard-col-left">
          <div className="card">
            <div className="card-header border-b pb-4">
              <div className="flex items-center gap-2">
                <Building2 size={20} className="text-red-600" />
                <h3 className="card-title">Hospital Profile</h3>
              </div>
              {profile && (
                <button
                  onClick={() => setIsProfileModalOpen(true)}
                  className="edit-profile-btn"
                >
                  Edit Profile
                </button>
              )}
            </div>

            <div className="card-body">
              {profileLoading ? (
                <div className="loading-spinner-container py-8">
                  <div className="spinner" />
                  <p>Loading profile...</p>
                </div>
              ) : !profile ? (
                <div className="empty-profile-box">
                  <AlertCircle size={36} className="text-amber-500 mb-2" />
                  <h4>Hospital Profile Required</h4>
                  <p className="text-xs text-slate-500 mb-4">
                    Register your hospital profile and license details before issuing blood requests.
                  </p>
                  <button
                    onClick={() => setIsProfileModalOpen(true)}
                    className="primary-btn w-full text-sm py-2"
                  >
                    Set Up Hospital Profile
                  </button>
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="p-3 bg-red-50/60 rounded-xl border border-red-100">
                    <h4 className="font-bold text-slate-900 text-base">{profile.hospitalName}</h4>
                    <p className="text-xs text-slate-500 flex items-center gap-1 mt-1">
                      <ShieldCheck size={14} className="text-emerald-600" /> Reg: <span className="font-mono">{profile.registrationNumber}</span>
                    </p>
                  </div>

                  <div className="text-sm space-y-2 text-slate-600">
                    <div className="flex items-center gap-2">
                      <MapPin size={16} className="text-slate-400 shrink-0" />
                      <span>{profile.address}, {profile.city}</span>
                    </div>

                    <div className="flex items-center gap-2">
                      <Phone size={16} className="text-slate-400 shrink-0" />
                      <span>{profile.contactNumber}</span>
                    </div>
                  </div>

                  {/* GPS Coordinates and Radius Indicator */}
                  <div className="gps-pill-card">
                    <div className="gps-pill-text">
                      <Compass size={16} className="text-blue-500 shrink-0" />
                      <span>GPS: <strong className="font-mono text-slate-800">{profile.latitude}, {profile.longitude}</strong></span>
                    </div>
                    <span className="radius-badge" title="Algorithmic search radius for matching donors">
                      20 km Radius
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Emergency Requests List & Filter */}
        <div className="dashboard-col-right">
          <div className="card">
            <div className="card-header border-b pb-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="card-title">Active Emergency Requests</h3>
                <p className="text-xs text-slate-500">Total Requests Issued: {totalElements}</p>
              </div>

              {/* Filter Controls */}
              <div className="flex flex-wrap gap-2 items-center">
                <select
                  className="filter-select text-xs py-1.5 px-2 border rounded-lg"
                  value={filterStatus}
                  onChange={(e) => {
                    setFilterStatus(e.target.value);
                    setCurrentPage(0);
                  }}
                >
                  <option value="">All Statuses</option>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="FULFILLED">FULFILLED</option>
                  <option value="CANCELLED">CANCELLED</option>
                </select>

                <select
                  className="filter-select text-xs py-1.5 px-2 border rounded-lg"
                  value={filterUrgency}
                  onChange={(e) => {
                    setFilterUrgency(e.target.value);
                    setCurrentPage(0);
                  }}
                >
                  <option value="">All Urgencies</option>
                  <option value="CRITICAL">CRITICAL</option>
                  <option value="HIGH">HIGH</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="LOW">LOW</option>
                </select>

                <select
                  className="filter-select text-xs py-1.5 px-2 border rounded-lg"
                  value={filterBloodGroup}
                  onChange={(e) => {
                    setFilterBloodGroup(e.target.value);
                    setCurrentPage(0);
                  }}
                >
                  <option value="">All Blood Groups</option>
                  {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((bg) => (
                    <option key={bg} value={bg}>{bg}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="card-body">
              {requestsLoading ? (
                <div className="loading-spinner-container py-12">
                  <div className="spinner" />
                  <p>Fetching emergency blood requests...</p>
                </div>
              ) : requests.length === 0 ? (
                <div className="empty-state py-12">
                  <Building2 size={48} className="empty-icon text-slate-300" />
                  <p className="empty-title">No Emergency Requests Found</p>
                  <p className="empty-desc">Click "New Emergency Request" to broadcast an urgent request for blood units.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {requests.map((req) => (
                    <div key={req.id} className="request-list-item">
                      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3">
                        <div className="flex items-center gap-3">
                          <span className="blood-group-tag-lg">{req.bloodGroup}</span>
                          <div>
                            <div className="flex flex-wrap items-center gap-2">
                              <h4 className="font-bold text-slate-900 text-sm">
                                Request #{req.id}
                              </h4>
                              <span className="text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded-md font-semibold">
                                {req.unitsRequired} Unit{req.unitsRequired > 1 ? 's' : ''}
                              </span>
                              <UrgencyBadge urgency={req.urgency} />
                            </div>
                            <span className="text-xs text-slate-500 flex items-center gap-1 mt-1">
                              <Clock size={13} /> Issued on {new Date(req.createdAt).toLocaleString()}
                            </span>
                          </div>
                        </div>

                        <RequestStatusBadge status={req.status} />
                      </div>

                      {/* Request Action Controls */}
                      <div className="mt-3 pt-3 border-t flex flex-wrap items-center justify-between gap-3 text-xs">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleFindMatches(req.id)}
                            className="btn-action-primary"
                            title="Execute algorithmic match search"
                          >
                            <Users size={14} /> Find Matches
                          </button>

                          <button
                            onClick={() => handleViewMatches(req.id)}
                            className="btn-action-secondary"
                            title="View all matched donors for this request"
                          >
                            <Eye size={14} /> View Donors
                          </button>
                        </div>

                        {req.status === 'ACTIVE' && (
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => handleUpdateStatus(req.id, 'FULFILLED')}
                              className="btn-action-fulfill"
                              title="Mark emergency request as fulfilled"
                            >
                              <Check size={14} /> Mark Fulfilled
                            </button>
                            <button
                              onClick={() => handleUpdateStatus(req.id, 'CANCELLED')}
                              className="btn-action-cancel"
                              title="Cancel this emergency request"
                            >
                              <X size={14} /> Cancel
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}

                  {/* Pagination Controls */}
                  {totalPages > 1 && (
                    <div className="flex items-center justify-between pt-4 border-t text-xs text-slate-600">
                      <span>Page {currentPage + 1} of {totalPages}</span>
                      <div className="flex gap-2">
                        <button
                          disabled={currentPage === 0}
                          onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                          className="px-3 py-1 border rounded disabled:opacity-40"
                        >
                          <ChevronLeft size={14} /> Previous
                        </button>
                        <button
                          disabled={currentPage >= totalPages - 1}
                          onClick={() => setCurrentPage((prev) => prev + 1)}
                          className="px-3 py-1 border rounded disabled:opacity-40"
                        >
                          Next <ChevronRight size={14} />
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Hospital Profile Modal */}
      <Modal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
        title={profile ? 'Update Hospital Profile' : 'Register Hospital Profile'}
      >
        <form onSubmit={handleSaveProfile} className="space-y-4">
          {profileModalError && (
            <div className="alert alert-error text-xs p-3">
              <AlertCircle size={16} /> <span>{profileModalError}</span>
            </div>
          )}

          <div className="form-group mb-0">
            <label className="form-label">Hospital / Institution Name</label>
            <input
              type="text"
              required
              className="form-input"
              placeholder="City General Hospital"
              value={profileForm.hospitalName}
              onChange={(e) => setProfileForm({ ...profileForm, hospitalName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="form-group mb-0">
              <label className="form-label">Medical Registration No.</label>
              <input
                type="text"
                required
                className="form-input"
                placeholder="REG-994821"
                value={profileForm.registrationNumber}
                onChange={(e) => setProfileForm({ ...profileForm, registrationNumber: e.target.value })}
              />
            </div>

            <div className="form-group mb-0">
              <label className="form-label">Emergency Phone</label>
              <input
                type="text"
                required
                className="form-input"
                placeholder="+1 800 555 0192"
                value={profileForm.contactNumber}
                onChange={(e) => setProfileForm({ ...profileForm, contactNumber: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group mb-0">
            <label className="form-label">City / District</label>
            <input
              type="text"
              required
              className="form-input"
              placeholder="Metropolis"
              value={profileForm.city}
              onChange={(e) => setProfileForm({ ...profileForm, city: e.target.value })}
            />
          </div>

          <div className="form-group mb-0">
            <label className="form-label">Full Hospital Address</label>
            <textarea
              required
              rows={2}
              className="form-input"
              placeholder="450 Healthcare Boulevard"
              value={profileForm.address}
              onChange={(e) => setProfileForm({ ...profileForm, address: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="form-group mb-0">
              <label className="form-label">Latitude Coordinate</label>
              <input
                type="number"
                step="any"
                className="form-input"
                placeholder="e.g. 12.9716"
                value={profileForm.latitude}
                onChange={(e) => setProfileForm({ ...profileForm, latitude: parseFloat(e.target.value) || 0 })}
              />
            </div>

            <div className="form-group mb-0">
              <label className="form-label">Longitude Coordinate</label>
              <input
                type="number"
                step="any"
                className="form-input"
                placeholder="e.g. 77.5946"
                value={profileForm.longitude}
                onChange={(e) => setProfileForm({ ...profileForm, longitude: parseFloat(e.target.value) || 0 })}
              />
            </div>
          </div>

          {/* Quick Location Tools */}
          <div className="geo-btn-row">
            <button
              type="button"
              onClick={handleDetectLocation}
              className="geo-action-btn geo-action-btn-primary"
            >
              <LocateFixed size={14} /> Detect My GPS Location
            </button>
            <button
              type="button"
              onClick={handleSetDemoLocation}
              className="geo-action-btn"
            >
              <MapPin size={14} /> Set Demo Location (Bangalore)
            </button>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            💡 <strong>Search Radius:</strong> Donors must be within <strong>20.0 km</strong> of the hospital coordinates to match. (Note: 1° difference ≈ 111 km, so coordinates should differ by less than 0.1°).
          </p>

          <div className="modal-footer">
            <button
              type="button"
              onClick={() => setIsProfileModalOpen(false)}
              className="modal-btn-cancel"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={profileSubmitting}
              className="modal-btn-submit"
            >
              {profileSubmitting ? 'Saving...' : 'Save Profile'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Create Emergency Request Modal */}
      <Modal
        isOpen={isCreateRequestModalOpen}
        onClose={() => setIsCreateRequestModalOpen(false)}
        title="Broadcast Emergency Blood Request"
      >
        <form onSubmit={handleCreateRequest} className="space-y-4">
          {requestModalError && (
            <div className="alert alert-error text-xs p-3">
              <AlertCircle size={16} /> <span>{requestModalError}</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="form-group mb-0">
              <label className="form-label">Required Blood Group</label>
              <select
                className="form-input"
                value={createRequestForm.bloodGroup}
                onChange={(e) => setCreateRequestForm({ ...createRequestForm, bloodGroup: e.target.value })}
              >
                {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((bg) => (
                  <option key={bg} value={bg}>{bg}</option>
                ))}
              </select>
            </div>

            <div className="form-group mb-0">
              <label className="form-label">Units Needed</label>
              <input
                type="number"
                min={1}
                required
                className="form-input"
                value={createRequestForm.unitsRequired}
                onChange={(e) => setCreateRequestForm({ ...createRequestForm, unitsRequired: parseInt(e.target.value) || 1 })}
              />
            </div>
          </div>

          <div className="form-group mb-0">
            <label className="form-label">Emergency Urgency Level</label>
            <select
              className="form-input"
              value={createRequestForm.urgency}
              onChange={(e) => setCreateRequestForm({ ...createRequestForm, urgency: e.target.value })}
            >
              <option value="CRITICAL">🚨 CRITICAL (Immediate Trauma Response)</option>
              <option value="HIGH">⚠️ HIGH (Urgent Surgery / 2-Hour Limit)</option>
              <option value="MEDIUM">⚡ MEDIUM (Same-Day Standard Need)</option>
              <option value="LOW">ℹ️ LOW (Elective Scheduling)</option>
            </select>
          </div>

          {/* Dynamic Blood Compatibility Notice */}
          <div className="compatibility-box">
            <div className="flex items-start gap-2">
              <Info size={16} className="shrink-0 mt-0.5 text-blue-600" />
              <div>
                <strong>Blood Compatibility Rule:</strong>
                {createRequestForm.bloodGroup === 'O-' && (
                  <p className="mt-0.5">⚠️ <strong>O- is highly restrictive:</strong> Only <strong>O-</strong> donors can donate to an O- patient. (O+ donors cannot donate to O- recipients).</p>
                )}
                {createRequestForm.bloodGroup === 'O+' && (
                  <p className="mt-0.5">Compatible donor blood groups: <strong>O-</strong> and <strong>O+</strong>.</p>
                )}
                {createRequestForm.bloodGroup === 'A+' && (
                  <p className="mt-0.5">Compatible donor blood groups: <strong>O-</strong>, <strong>O+</strong>, <strong>A-</strong>, and <strong>A+</strong>.</p>
                )}
                {createRequestForm.bloodGroup === 'A-' && (
                  <p className="mt-0.5">Compatible donor blood groups: <strong>O-</strong> and <strong>A-</strong>.</p>
                )}
                {createRequestForm.bloodGroup === 'B+' && (
                  <p className="mt-0.5">Compatible donor blood groups: <strong>O-</strong>, <strong>O+</strong>, <strong>B-</strong>, and <strong>B+</strong>.</p>
                )}
                {createRequestForm.bloodGroup === 'B-' && (
                  <p className="mt-0.5">Compatible donor blood groups: <strong>O-</strong> and <strong>B-</strong>.</p>
                )}
                {createRequestForm.bloodGroup === 'AB+' && (
                  <p className="mt-0.5">🌟 <strong>Universal recipient:</strong> Can receive from <strong>all blood groups</strong> (O-, O+, A-, A+, B-, B+, AB-, AB+).</p>
                )}
                {createRequestForm.bloodGroup === 'AB-' && (
                  <p className="mt-0.5">Compatible donor blood groups: <strong>O-</strong>, <strong>A-</strong>, <strong>B-</strong>, and <strong>AB-</strong>.</p>
                )}
              </div>
            </div>
          </div>

          <div className="form-group-note">
            <strong>Note:</strong> Creating this request broadcasts an alert to registered donors within <strong>20.0 km</strong> who match the compatibility rule above.
          </div>

          <div className="modal-footer">
            <button
              type="button"
              onClick={() => setIsCreateRequestModalOpen(false)}
              className="modal-btn-cancel"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={requestSubmitting}
              className="modal-btn-submit"
            >
              {requestSubmitting ? 'Broadcasting...' : 'Broadcast Request'}
            </button>
          </div>
        </form>
      </Modal>

      {/* View / Manage Matched Donors Modal */}
      <Modal
        isOpen={isMatchesModalOpen}
        onClose={() => setIsMatchesModalOpen(false)}
        title={`Matched Donors for Request #${activeRequestForMatches}`}
        maxWidth="max-w-2xl"
      >
        {matchesModalLoading ? (
          <div className="loading-spinner-container py-8">
            <div className="spinner" />
            <p>Scanning donors within 20 km radius...</p>
          </div>
        ) : matchedDonors.length === 0 ? (
          <div className="empty-state py-8">
            <Users size={36} className="text-slate-300" />
            <h4 className="empty-title text-base font-bold text-slate-800 mt-2">No Matching Donors Found</h4>
            <p className="empty-desc text-xs text-slate-500 max-w-md mx-auto mb-4">
              No donors matching both the blood compatibility matrix and the <strong>20.0 km radius</strong> were found.
            </p>

            <div className="bg-slate-50 border rounded-xl p-4 text-xs text-left text-slate-700 max-w-md mx-auto space-y-2">
              <div className="font-bold text-slate-900 border-b pb-1">Why did no donors match?</div>
              <div className="flex items-start gap-2">
                <span className="text-blue-600 font-bold shrink-0">1. Blood Compatibility:</span>
                <span>The donor must be compatible (e.g. O- requests only match O- donors; O+ donors cannot donate to O-).</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="text-emerald-600 font-bold shrink-0">2. Emergency Availability:</span>
                <span>The donor must have toggled <strong>AVAILABLE TO DONATE</strong> on their dashboard.</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="text-amber-600 font-bold shrink-0">3. 20 km Distance:</span>
                <span>Hospital GPS: <span className="font-mono bg-white px-1 py-0.5 border rounded">{profile?.latitude}, {profile?.longitude}</span>. Coordinates must be within 20 km (differ by less than ~0.1°).</span>
              </div>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {matchedDonors.map((m) => (
              <div key={m.id} className="p-4 border border-slate-200 rounded-xl bg-white shadow-sm space-y-3">
                {/* Donor Header */}
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="font-bold text-slate-900 text-sm">
                        {m.donorName || `Donor #${m.donorId}`}
                      </h4>
                      <span className="badge badge-active">{m.bloodGroup}</span>
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      Match ID: <span className="font-mono font-medium text-slate-700">{m.id}</span> • Donor ID: <span className="font-mono font-medium text-slate-700">{m.donorId}</span>
                      {m.createdAt && <span> • Matched at {new Date(m.createdAt).toLocaleTimeString()}</span>}
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <MatchStatusBadge status={m.status} />
                  </div>
                </div>

                {/* Status-dependent Contact & Action Section */}
                {m.status === 'ACCEPTED' ? (
                  <div className="p-3 bg-emerald-50/80 border border-emerald-200 rounded-xl space-y-2.5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-900">
                        <ShieldCheck size={16} className="text-emerald-700" />
                        <span>Accepted with Verified Contact Sharing</span>
                      </div>
                      <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded uppercase tracking-wider">
                        Donor Ready to Coordinate
                      </span>
                    </div>

                    {m.contactNumber || m.email ? (
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                        {m.contactNumber && (
                          <div className="flex items-center justify-between gap-2 p-2 bg-white rounded-lg border border-emerald-100 text-xs">
                            <div className="flex items-center gap-2 truncate">
                              <Phone size={14} className="text-emerald-600 shrink-0" />
                              <span className="font-mono font-medium text-slate-800 truncate">{m.contactNumber}</span>
                            </div>
                            <a
                              href={`tel:${m.contactNumber}`}
                              className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded text-[11px] shrink-0 inline-flex items-center gap-1 transition shadow-xs"
                              title="Call donor directly"
                            >
                              <Phone size={11} /> Call
                            </a>
                          </div>
                        )}

                        {m.email && (
                          <div className="flex items-center justify-between gap-2 p-2 bg-white rounded-lg border border-emerald-100 text-xs">
                            <div className="flex items-center gap-2 truncate">
                              <Mail size={14} className="text-emerald-600 shrink-0" />
                              <span className="font-mono font-medium text-slate-800 truncate" title={m.email}>{m.email}</span>
                            </div>
                            <a
                              href={`mailto:${m.email}?subject=LifeLink Emergency Blood Request %23${activeRequestForMatches}&body=Hello ${m.donorName || 'Donor'},%0D%0A%0D%0AThank you for accepting our emergency request for blood group ${m.bloodGroup}. Please reply or call us to coordinate arrival and donation screening.%0D%0A%0D%0AThank you,%0D%0A${profile?.hospitalName || 'Hospital Medical Staff'}`}
                              className="px-2.5 py-1 bg-slate-800 hover:bg-slate-900 text-white font-semibold rounded text-[11px] shrink-0 inline-flex items-center gap-1 transition shadow-xs"
                              title="Send email to donor"
                            >
                              <Mail size={11} /> Email
                            </a>
                          </div>
                        )}
                      </div>
                    ) : (
                      <p className="text-xs text-slate-600 italic">
                        Donor accepted without contact sharing consent. Please coordinate via standard hospital procedures.
                      </p>
                    )}

                    <div className="flex items-center justify-between pt-2 border-t border-emerald-200/60">
                      <span className="text-[11px] text-slate-500">
                        Once donor arrives and donation is fulfilled:
                      </span>
                      <button
                        onClick={() => handleCompleteMatch(m.id)}
                        className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg transition inline-flex items-center gap-1 shadow-sm"
                      >
                        <Check size={13} /> Complete Donation
                      </button>
                    </div>
                  </div>
                ) : m.status === 'COMPLETED' ? (
                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs flex items-center justify-between text-blue-900">
                    <div className="flex items-center gap-2">
                      <Check size={16} className="text-blue-600" />
                      <div>
                        <span className="font-bold block">Donation Completed & Verified</span>
                        <span className="text-[11px] text-blue-700">Donation fulfilled by this donor. Units accounted for.</span>
                      </div>
                    </div>
                    {m.contactNumber && (
                      <span className="font-mono text-[11px] text-slate-600">Contact: {m.contactNumber}</span>
                    )}
                  </div>
                ) : m.status === 'REJECTED' ? (
                  <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-500 flex items-center justify-between">
                    <span className="flex items-center gap-1.5">
                      <X size={14} className="text-red-400" />
                      Donor declined this match.
                    </span>
                    <span className="text-[11px] text-slate-400 font-mono">🔒 Contact details hidden</span>
                  </div>
                ) : (
                  <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-600 flex items-center justify-between">
                    <span className="flex items-center gap-1.5">
                      <Clock size={14} className="text-amber-500" />
                      Awaiting donor response. Match notification delivered.
                    </span>
                    <span className="text-[11px] text-slate-500 font-mono bg-white px-2 py-0.5 border rounded">
                      🔒 Contact hidden until accepted
                    </span>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </Modal>
    </div>
  );
}
