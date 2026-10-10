import React, { useState, useEffect, useCallback } from 'react';
import { 
  Heart, 
  MapPin, 
  Phone, 
  Droplet, 
  Check, 
  X, 
  Edit3, 
  AlertCircle, 
  CheckCircle2, 
  Clock, 
  RefreshCw,
  UserCheck,
  Compass,
  LocateFixed,
  ShieldCheck,
  Building2
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { Modal } from '../components/Modal';
import { MatchStatusBadge, AvailabilityBadge } from '../components/Badges';
import { getRequestDetails } from '../utils/requestRegistry';

export function DonorDashboard() {
  const { user } = useAuth();
  const { showToast, fetchNotifications } = useNotifications();

  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState(null);

  const [matches, setMatches] = useState([]);
  const [matchesLoading, setMatchesLoading] = useState(true);

  // Accept / Consent Modal State
  const [acceptingMatch, setAcceptingMatch] = useState(null);
  const [isAcceptModalOpen, setIsAcceptModalOpen] = useState(false);
  const [consentToShareContact, setConsentToShareContact] = useState(true);
  const [actionInProgressId, setActionInProgressId] = useState(null);

  // Edit/Create profile modal
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [profileFormData, setProfileFormData] = useState({
    bloodGroup: 'O+',
    address: '',
    city: '',
    latitude: 0.0,
    longitude: 0.0,
    contactNumber: '',
    available: true,
  });
  const [modalSubmitting, setModalSubmitting] = useState(false);
  const [modalError, setModalError] = useState('');

  // Fetch Donor Profile
  const fetchProfile = useCallback(async () => {
    setProfileLoading(true);
    setProfileError(null);
    try {
      const data = await api.donor.getProfile();
      setProfile(data);
      setProfileFormData({
        bloodGroup: data.bloodGroup || 'O+',
        address: data.address || '',
        city: data.city || '',
        latitude: data.latitude || 0.0,
        longitude: data.longitude || 0.0,
        contactNumber: data.contactNumber || '',
        available: data.available !== false,
      });
    } catch (err) {
      // Backend throws exception if donor profile does not exist yet
      setProfile(null);
    } finally {
      setProfileLoading(false);
    }
  }, []);

  // Fetch Donor Matches
  const fetchMatches = useCallback(async () => {
    setMatchesLoading(true);
    try {
      const data = await api.donor.getMyMatches();
      setMatches(data || []);
    } catch (err) {
      console.error('Failed to fetch donor matches:', err);
    } finally {
      setMatchesLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfile();
    fetchMatches();
  }, [fetchProfile, fetchMatches]);

  // Handle Availability Toggle
  const handleToggleAvailability = async () => {
    if (!profile) return;
    const newStatus = !profile.available;
    try {
      const updated = await api.donor.updateAvailability(newStatus);
      setProfile(updated);
      showToast(
        'Availability Updated',
        newStatus
          ? 'You are now marked as AVAILABLE for emergency requests.'
          : 'You are now marked as UNAVAILABLE.',
        newStatus ? 'success' : 'info'
      );
    } catch (err) {
      showToast('Error', err.message || 'Failed to update availability', 'error');
    }
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
        setProfileFormData((prev) => ({
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

  // Set demo location (approx 0.6 km from hospital test location)
  const handleSetDemoLocation = () => {
    setProfileFormData((prev) => ({
      ...prev,
      city: prev.city || 'Bangalore',
      latitude: 12.9750,
      longitude: 77.5990,
    }));
    showToast('Demo Location Set', 'Donor coordinates set to 12.9750, 77.5990 (just 0.6 km from hospital demo location!). Matches will trigger immediately.', 'info');
  };

  // Handle Profile Save (Create or Update)
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setModalError('');

    if (!profileFormData.address.trim() || !profileFormData.city.trim() || !profileFormData.contactNumber.trim()) {
      setModalError('Please fill in address, city, and contact number.');
      return;
    }

    const payload = {
      bloodGroup: profileFormData.bloodGroup,
      address: profileFormData.address.trim(),
      city: profileFormData.city.trim(),
      contactNumber: profileFormData.contactNumber.trim(),
      latitude: parseFloat(profileFormData.latitude) || 0.0,
      longitude: parseFloat(profileFormData.longitude) || 0.0,
      available: !!profileFormData.available,
    };

    setModalSubmitting(true);
    try {
      let saved;
      if (profile && profile.id) {
        saved = await api.donor.updateProfile(payload);
        showToast('Profile Updated', 'Your donor profile has been updated successfully.', 'success');
      } else {
        saved = await api.donor.createProfile(payload);
        showToast('Profile Created', 'Welcome to the LifeLink donor network!', 'success');
      }
      setProfile(saved);
      setIsProfileModalOpen(false);
      fetchMatches();
    } catch (err) {
      setModalError(err.message || 'Failed to save profile.');
    } finally {
      setModalSubmitting(false);
    }
  };

  // Open Donor Acceptance Modal
  const handleOpenAcceptModal = (match) => {
    setAcceptingMatch(match);
    setConsentToShareContact(true);
    setIsAcceptModalOpen(true);
  };

  // Confirm Donor Acceptance with Consent
  const handleConfirmAccept = async () => {
    if (!acceptingMatch) return;
    setActionInProgressId(acceptingMatch.id);
    try {
      await api.emergency.updateMatchStatus(
        acceptingMatch.id,
        'ACCEPTED',
        consentToShareContact
      );
      showToast(
        'Match Accepted',
        consentToShareContact
          ? 'Thank you! Your acceptance and contact details have been shared with the hospital for coordination.'
          : 'Thank you! Your acceptance has been registered with the hospital.',
        'success'
      );
      setIsAcceptModalOpen(false);
      setAcceptingMatch(null);
      await Promise.all([fetchMatches(), fetchNotifications()]);
    } catch (err) {
      showToast('Acceptance Failed', err.message || 'Failed to accept request.', 'error');
    } finally {
      setActionInProgressId(null);
    }
  };

  // Handle Decline Match
  const handleDeclineMatch = async (match) => {
    if (actionInProgressId === match.id) return;
    const confirmDecline = window.confirm(
      `Are you sure you want to decline Emergency Request #${match.emergencyRequestId}? Other nearby donors may be notified.`
    );
    if (!confirmDecline) return;

    setActionInProgressId(match.id);
    try {
      await api.emergency.updateMatchStatus(match.id, 'REJECTED', false);
      showToast('Request Declined', 'You have declined this emergency match.', 'info');
      await Promise.all([fetchMatches(), fetchNotifications()]);
    } catch (err) {
      showToast('Decline Failed', err.message || 'Failed to decline request.', 'error');
    } finally {
      setActionInProgressId(null);
    }
  };

  return (
    <div className="dashboard-container">
      {/* Header Banner */}
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-title">Donor Control Center</h1>
          <p className="dashboard-subtitle">
            Welcome back, <strong>{user?.name}</strong>. Manage your availability and emergency donation matches.
          </p>
        </div>

        <button
          onClick={() => {
            fetchProfile();
            fetchMatches();
          }}
          className="refresh-btn"
          title="Refresh Data"
        >
          <RefreshCw size={16} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Main Grid */}
      <div className="dashboard-grid">
        {/* Left Column: Profile Card */}
        <div className="dashboard-col-left">
          <div className="card profile-card">
            <div className="card-header border-b pb-4">
              <div className="flex items-center gap-2">
                <Heart size={20} className="text-red-600 fill-red-600" />
                <h3 className="card-title">Donor Profile</h3>
              </div>
              {profile && (
                <button
                  onClick={() => setIsProfileModalOpen(true)}
                  className="edit-profile-btn"
                >
                  <Edit3 size={14} /> Edit
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
                  <h4>Profile Incomplete</h4>
                  <p className="text-xs text-slate-500 mb-4">
                    Create your donor profile with blood group and contact details to begin receiving emergency match requests.
                  </p>
                  <button
                    onClick={() => setIsProfileModalOpen(true)}
                    className="primary-btn w-full text-sm py-2"
                  >
                    Set Up Donor Profile
                  </button>
                </div>
              ) : (
                <div className="profile-details space-y-4">
                  {/* Blood Group Display */}
                  <div className="blood-group-hero-box">
                    <div className="blood-group-badge">{profile.bloodGroup}</div>
                    <div>
                      <div className="text-xs text-slate-500 uppercase tracking-wider font-semibold">Blood Group</div>
                      <div className="text-sm font-semibold text-slate-800">Verified Donor</div>
                    </div>
                  </div>

                  {/* Availability Toggle Box */}
                  <div className="availability-toggle-box">
                    <div>
                      <div className="text-xs text-slate-500 uppercase font-semibold">Emergency Status</div>
                      <AvailabilityBadge available={profile.available} />
                    </div>

                    <button
                      onClick={handleToggleAvailability}
                      className={`toggle-switch-btn ${profile.available ? 'active' : ''}`}
                    >
                      <span className="toggle-slider" />
                    </button>
                  </div>

                  {/* Location & Contact Info */}
                  <div className="info-list space-y-2 pt-2 border-t text-sm">
                    <div className="flex items-center gap-2 text-slate-600">
                      <MapPin size={16} className="text-slate-400 shrink-0" />
                      <span>{profile.address}, {profile.city}</span>
                    </div>

                    <div className="flex items-center gap-2 text-slate-600">
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
                    <span className="radius-badge" title="Search radius for emergency hospital requests">
                      20 km Radius
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Emergency Matches List */}
        <div className="dashboard-col-right">
          <div className="card">
            <div className="card-header border-b pb-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Droplet size={20} className="text-red-600" />
                  <h3 className="card-title">Emergency Donation Matches</h3>
                </div>
                <span className="badge badge-active">{matches.length} Matches</span>
              </div>
            </div>

            <div className="card-body">
              {matchesLoading ? (
                <div className="loading-spinner-container py-12">
                  <div className="spinner" />
                  <p>Searching for emergency matches...</p>
                </div>
              ) : matches.length === 0 ? (
                <div className="empty-state py-12">
                  <UserCheck size={48} className="empty-icon text-slate-300" />
                  <p className="empty-title">No Active Matches Yet</p>
                  <p className="empty-desc">
                    When a hospital posts an emergency request matching your blood type <strong>({profile?.bloodGroup || 'Set profile'})</strong>, it will appear here instantly.
                  </p>
                </div>
              ) : (
                <div className="matches-list space-y-4">
                  {matches.map((match) => {
                    const details = getRequestDetails(match.emergencyRequestId);
                    return (
                      <div key={match.id} className="match-card">
                        <div className="match-card-header">
                          <div className="flex items-center gap-3">
                            <span className="match-blood-tag">{match.bloodGroup}</span>
                            <div>
                              <div className="flex items-center gap-2">
                                <h4 className="font-semibold text-slate-900 text-sm">
                                  Request #{match.emergencyRequestId}
                                </h4>
                                {details?.urgency && (
                                  <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${details.urgency === 'CRITICAL' ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'}`}>
                                    {details.urgency}
                                  </span>
                                )}
                              </div>
                              <div className="text-xs text-red-700 font-semibold flex items-center gap-1 mt-0.5">
                                <Building2 size={13} className="shrink-0" />
                                <span>Demanding Hospital: <strong className="text-slate-900">{details.hospitalName}</strong> {details.city ? `(${details.city})` : ''}</span>
                              </div>
                              <span className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                                <Clock size={12} /> Matched on {new Date(match.createdAt).toLocaleDateString()}
                              </span>
                            </div>
                          </div>

                          <MatchStatusBadge status={match.status} />
                        </div>

                      <div className="match-card-body mt-3 pt-3 border-t text-sm">
                        <div className="flex justify-between items-center text-xs text-slate-500 mb-2">
                          <div>
                            Donor ID: <span className="font-mono font-semibold text-slate-700">{match.donorId}</span>
                          </div>
                          <div>
                            Match ID: <span className="font-mono font-semibold text-slate-700">{match.id}</span>
                          </div>
                        </div>

                        {match.status === 'MATCHED' || match.status === 'NOTIFIED' ? (
                          <div className="flex items-center justify-between gap-3 pt-1">
                            <span className="text-xs text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-md font-medium">
                              Emergency match: Willing to coordinate for this patient?
                            </span>
                            <div className="flex gap-2 shrink-0">
                              <button
                                onClick={() => handleOpenAcceptModal(match)}
                                disabled={actionInProgressId === match.id}
                                className="btn-accept"
                                title="Accept and review contact sharing"
                              >
                                <Check size={14} /> Accept
                              </button>
                              <button
                                onClick={() => handleDeclineMatch(match)}
                                disabled={actionInProgressId === match.id}
                                className="btn-reject"
                                title="Decline this emergency request"
                              >
                                <X size={14} /> Decline
                              </button>
                            </div>
                          </div>
                        ) : match.status === 'ACCEPTED' ? (
                          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl space-y-1.5">
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-bold text-emerald-900 flex items-center gap-1.5">
                                <CheckCircle2 size={16} className="text-emerald-600" />
                                Response Registered: Accepted
                              </span>
                              <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-200 text-emerald-900 rounded uppercase">
                                Coordination Active
                              </span>
                            </div>
                            <p className="text-xs text-emerald-800">
                              {match.contactSharingConsent
                                ? '✓ Consent granted: Your registered phone and email are securely visible to the requesting hospital team for coordination.'
                                : 'Accepted without contact sharing consent.'}
                            </p>
                            <p className="text-[11px] text-slate-500 pt-1 border-t border-emerald-200/70">
                              ℹ️ Acceptance indicates willingness to coordinate. Hospital medical staff will contact you directly to confirm timing, screening, and donation.
                            </p>
                          </div>
                        ) : match.status === 'COMPLETED' ? (
                          <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl flex items-center gap-2 text-xs text-blue-900">
                            <CheckCircle2 size={16} className="text-blue-600 shrink-0" />
                            <div>
                              <span className="font-bold block">Donation Completed & Verified</span>
                              <span className="text-[11px] text-blue-700">The hospital has marked this emergency donation as completed. Thank you for your lifesaving support!</span>
                            </div>
                          </div>
                        ) : match.status === 'REJECTED' ? (
                          <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl flex items-center gap-2 text-xs text-slate-600">
                            <X size={15} className="text-slate-400 shrink-0" />
                            <span>You declined this emergency request match.</span>
                          </div>
                        ) : null}
                      </div>
                    </div>
                  );
                })}
              </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Edit/Create Profile Modal */}
      <Modal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
        title={profile ? 'Update Donor Profile' : 'Create Donor Profile'}
      >
        <form onSubmit={handleSaveProfile} className="space-y-4">
          {modalError && (
            <div className="alert alert-error text-xs p-3">
              <AlertCircle size={16} /> <span>{modalError}</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="form-group mb-0">
              <label className="form-label">Blood Group</label>
              <select
                className="form-input"
                value={profileFormData.bloodGroup}
                onChange={(e) => setProfileFormData({ ...profileFormData, bloodGroup: e.target.value })}
              >
                {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((bg) => (
                  <option key={bg} value={bg}>{bg}</option>
                ))}
              </select>
            </div>

            <div className="form-group mb-0">
              <label className="form-label">Contact Phone</label>
              <input
                type="text"
                required
                className="form-input"
                placeholder="+1 555 019 2831"
                value={profileFormData.contactNumber}
                onChange={(e) => setProfileFormData({ ...profileFormData, contactNumber: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group mb-0">
            <label className="form-label">City / Region</label>
            <input
              type="text"
              required
              className="form-input"
              placeholder="New York"
              value={profileFormData.city}
              onChange={(e) => setProfileFormData({ ...profileFormData, city: e.target.value })}
            />
          </div>

          <div className="form-group mb-0">
            <label className="form-label">Street Address</label>
            <textarea
              required
              rows={2}
              className="form-input"
              placeholder="123 Medical Center Way"
              value={profileFormData.address}
              onChange={(e) => setProfileFormData({ ...profileFormData, address: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="form-group mb-0">
              <label className="form-label">Latitude Coordinate</label>
              <input
                type="number"
                step="any"
                className="form-input"
                placeholder="e.g. 12.9750"
                value={profileFormData.latitude}
                onChange={(e) => setProfileFormData({ ...profileFormData, latitude: parseFloat(e.target.value) || 0 })}
              />
            </div>

            <div className="form-group mb-0">
              <label className="form-label">Longitude Coordinate</label>
              <input
                type="number"
                step="any"
                className="form-input"
                placeholder="e.g. 77.5990"
                value={profileFormData.longitude}
                onChange={(e) => setProfileFormData({ ...profileFormData, longitude: parseFloat(e.target.value) || 0 })}
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
            💡 <strong>Search Radius:</strong> Hospitals search within <strong>20.0 km</strong>. (Note: 1° difference ≈ 111 km, so coordinates should differ by less than 0.1°).
          </p>

          <div className="flex items-center gap-2 pt-2">
            <input
              type="checkbox"
              id="availCheck"
              className="w-4 h-4 text-red-600 rounded"
              checked={profileFormData.available}
              onChange={(e) => setProfileFormData({ ...profileFormData, available: e.target.checked })}
            />
            <label htmlFor="availCheck" className="text-sm font-medium text-slate-700">
              Available immediately for emergency donation calls
            </label>
          </div>

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
              disabled={modalSubmitting}
              className="modal-btn-submit"
            >
              {modalSubmitting ? 'Saving...' : 'Save Profile'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Accept & Contact Consent Modal */}
      <Modal
        isOpen={isAcceptModalOpen}
        onClose={() => !actionInProgressId && setIsAcceptModalOpen(false)}
        title="Confirm Emergency Request Acceptance"
      >
        {acceptingMatch && (
          <div className="space-y-4">
            <div className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Emergency Blood Request
                </span>
                <span className="font-bold text-red-700 bg-red-100 px-2 py-0.5 rounded text-xs">
                  Blood Group: {acceptingMatch.bloodGroup}
                </span>
              </div>
              <div className="text-sm font-bold text-slate-900">
                Request #{acceptingMatch.emergencyRequestId}
              </div>
              <div className="text-xs text-slate-700 flex items-center gap-1.5">
                <Building2 size={14} className="text-slate-500 shrink-0" />
                <span>
                  Demanding Hospital:{' '}
                  <strong>{getRequestDetails(acceptingMatch.emergencyRequestId)?.hospitalName || 'Verified Healthcare Facility'}</strong>
                  {getRequestDetails(acceptingMatch.emergencyRequestId)?.city ? ` (${getRequestDetails(acceptingMatch.emergencyRequestId).city})` : ''}
                </span>
              </div>
            </div>

            <div className="text-xs text-slate-600 leading-relaxed bg-amber-50/70 border border-amber-200 p-3 rounded-xl">
              <div className="font-bold text-amber-900 flex items-center gap-1.5 mb-1">
                <AlertCircle size={14} className="text-amber-700" />
                Coordination Commitment
              </div>
              By accepting, you indicate your availability and willingness to coordinate with the hospital medical staff. 
              <strong> This does not automatically complete the donation</strong>; clinical screening and donation take place directly at the hospital.
            </div>

            {/* Explicit Consent Box */}
            <div className="border border-emerald-300 bg-emerald-50/60 rounded-xl p-4 space-y-3">
              <div className="flex items-center gap-2 text-emerald-900 font-bold text-xs uppercase tracking-wide">
                <ShieldCheck size={16} className="text-emerald-700" />
                <span>Secure Contact Sharing Consent</span>
              </div>

              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  id="consentCheckbox"
                  checked={consentToShareContact}
                  onChange={(e) => setConsentToShareContact(e.target.checked)}
                  className="mt-1 h-4 w-4 rounded border-gray-300 text-red-600 focus:ring-red-500 cursor-pointer"
                />
                <div className="text-xs text-slate-800 leading-normal">
                  <span className="font-bold text-slate-900 block mb-0.5">
                    I consent to share my contact details for emergency coordination
                  </span>
                  I authorize LifeLink to release my registered phone number 
                  {profile?.contactNumber ? <strong> ({profile.contactNumber})</strong> : ''} and registered email address 
                  {user?.email ? <strong> ({user.email})</strong> : ''} exclusively to 
                  <strong> {getRequestDetails(acceptingMatch.emergencyRequestId)?.hospitalName || 'the requesting hospital'}</strong> so their medical team can call or email me directly.
                </div>
              </label>

              <div className="text-[11px] text-slate-500 pt-2 border-t border-emerald-200/60">
                🔒 <strong>Privacy Guarantee:</strong> Contact information is strictly restricted to this authorized hospital for this emergency request only. Unrelated hospitals, donors, and unauthenticated users cannot view your contact details.
              </div>
            </div>

            <div className="modal-footer pt-3">
              <button
                type="button"
                onClick={() => setIsAcceptModalOpen(false)}
                disabled={actionInProgressId !== null}
                className="modal-btn-cancel"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmAccept}
                disabled={actionInProgressId !== null}
                className="modal-btn-submit bg-emerald-600 hover:bg-emerald-700"
              >
                {actionInProgressId ? 'Processing...' : 'Confirm Acceptance & Share Contact'}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
