/**
 * DonorDashboard.test.jsx
 *
 * Tests for the Donor Control Center dashboard covering:
 * 1. Profile incomplete state → "Set Up Donor Profile" prompt
 * 2. Profile loaded state → blood group, availability badge, toggle button rendered
 * 3. Availability toggle → calls api.donor.updateAvailability with flipped value
 * 4. Emergency matches list → renders match cards with blood group and status
 * 5. Accept match → opens consent modal; Confirm Accept → calls updateMatchStatus
 * 6. Decline match → calls window.confirm then updateMatchStatus with REJECTED
 * 7. Empty matches state → "No Active Matches Yet" empty state shown
 */

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { DonorDashboard } from '../pages/DonorDashboard';

// ── Module mocks ──────────────────────────────────────────────────────────────

vi.mock('../api/client', () => ({
  api: {
    donor: {
      getProfile: vi.fn(),
      getMyMatches: vi.fn(),
      updateAvailability: vi.fn(),
    },
    emergency: {
      updateMatchStatus: vi.fn(),
    },
  },
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, name: 'Arjun Singh', email: 'arjun@example.com', role: 'DONOR' },
  }),
}));

vi.mock('../context/NotificationContext', () => ({
  useNotifications: () => ({
    showToast: vi.fn(),
    fetchNotifications: vi.fn(),
  }),
}));

vi.mock('../utils/requestRegistry', () => ({
  getRequestDetails: vi.fn(() => ({
    hospitalName: 'City Hospital',
    city: 'Bangalore',
    urgency: 'CRITICAL',
  })),
}));

// ── Helpers ───────────────────────────────────────────────────────────────────

import { api } from '../api/client';

const MOCK_PROFILE = {
  id: 10,
  bloodGroup: 'A+',
  address: '12 MG Road',
  city: 'Bangalore',
  contactNumber: '+91 9000000000',
  available: true,
  latitude: 12.975,
  longitude: 77.599,
};

const MOCK_MATCH = {
  id: 55,
  emergencyRequestId: 7,
  bloodGroup: 'A+',
  donorId: 10,
  status: 'MATCHED',
  contactSharingConsent: false,
  createdAt: new Date().toISOString(),
};

function renderDonorDashboard() {
  return render(<DonorDashboard />);
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('DonorDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── 1. Profile incomplete state ───────────────────────────────────────────

  describe('when donor profile does not exist', () => {
    it('shows Profile Incomplete prompt with Set Up button', async () => {
      api.donor.getProfile.mockRejectedValue(new Error('Profile not found'));
      api.donor.getMyMatches.mockResolvedValue([]);

      renderDonorDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Profile Incomplete/i)).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /Set Up Donor Profile/i })).toBeInTheDocument();
    });

    it('shows No Active Matches Yet when matches are empty and profile missing', async () => {
      api.donor.getProfile.mockRejectedValue(new Error('Not found'));
      api.donor.getMyMatches.mockResolvedValue([]);

      renderDonorDashboard();

      await waitFor(() => {
        expect(screen.getByText(/No Active Matches Yet/i)).toBeInTheDocument();
      });
    });
  });

  // ── 2. Profile loaded state ───────────────────────────────────────────────

  describe('when donor profile is loaded', () => {
    it('renders blood group, city, and contact number from profile', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([]);

      renderDonorDashboard();

      await waitFor(() => {
        expect(screen.getByText('A+')).toBeInTheDocument();
      });
      expect(screen.getByText(/Bangalore/)).toBeInTheDocument();
      expect(screen.getByText('+91 9000000000')).toBeInTheDocument();
    });

    it('renders the availability toggle switch button', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([]);

      renderDonorDashboard();

      await waitFor(() => {
        // The toggle renders as a button with class toggle-switch-btn
        expect(document.querySelector('.toggle-switch-btn')).toBeInTheDocument();
      });
    });
  });

  // ── 3. Availability toggle ────────────────────────────────────────────────

  describe('availability toggle', () => {
    it('calls api.donor.updateAvailability with flipped status when toggle is clicked', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE); // available: true
      api.donor.getMyMatches.mockResolvedValue([]);
      api.donor.updateAvailability.mockResolvedValue({ ...MOCK_PROFILE, available: false });

      renderDonorDashboard();

      await waitFor(() => {
        expect(document.querySelector('.toggle-switch-btn')).toBeInTheDocument();
      });

      fireEvent.click(document.querySelector('.toggle-switch-btn'));

      await waitFor(() => {
        // Profile was available:true → toggle calls with false
        expect(api.donor.updateAvailability).toHaveBeenCalledWith(false);
      });
    });
  });

  // ── 4. Matches list rendering ─────────────────────────────────────────────

  describe('emergency matches list', () => {
    it('renders a match card with blood group and request number', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([MOCK_MATCH]);

      renderDonorDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Request #7/i)).toBeInTheDocument();
      });
      // Blood group shown in match card
      expect(screen.getAllByText('A+').length).toBeGreaterThan(0);
    });

    it('shows match count badge', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([MOCK_MATCH]);

      renderDonorDashboard();

      await waitFor(() => {
        expect(screen.getByText(/1 Matches/i)).toBeInTheDocument();
      });
    });

    it('shows Accept and Decline buttons for MATCHED status', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([MOCK_MATCH]);

      renderDonorDashboard();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Accept/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Decline/i })).toBeInTheDocument();
      });
    });
  });

  // ── 5. Accept match → consent modal ──────────────────────────────────────

  describe('accept match flow', () => {
    it('opens consent modal when Accept button is clicked', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([MOCK_MATCH]);

      renderDonorDashboard();

      await waitFor(() => screen.getByRole('button', { name: /Accept/i }));
      fireEvent.click(screen.getByRole('button', { name: /Accept/i }));

      await waitFor(() => {
        expect(screen.getByText(/Confirm Emergency Request Acceptance/i)).toBeInTheDocument();
      });
    });

    it('calls updateMatchStatus with ACCEPTED when Confirm button is clicked', async () => {
      // First call returns match, subsequent calls (after accept) return empty list
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches
        .mockResolvedValueOnce([MOCK_MATCH])  // initial load
        .mockResolvedValue([]);               // after accept re-fetch
      api.emergency.updateMatchStatus.mockResolvedValue({});

      renderDonorDashboard();

      // Wait for initial render with Accept button
      const acceptBtn = await waitFor(() => screen.getByRole('button', { name: /^Accept$/i }));
      fireEvent.click(acceptBtn);

      // Consent modal opens
      await waitFor(() => screen.getByText(/Confirm Emergency Request Acceptance/i));

      // Click the confirm button in the modal
      const confirmBtn = screen.getByRole('button', { name: /Confirm Acceptance/i });
      fireEvent.click(confirmBtn);

      await waitFor(() => {
        expect(api.emergency.updateMatchStatus).toHaveBeenCalledWith(55, 'ACCEPTED', true);
      });
    });
  });

  // ── 6. Decline match flow ─────────────────────────────────────────────────

  describe('decline match flow', () => {
    it('calls updateMatchStatus with REJECTED after user confirms window.confirm', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([MOCK_MATCH]);
      api.emergency.updateMatchStatus.mockResolvedValue({});

      // Stub window.confirm to auto-approve
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

      renderDonorDashboard();

      await waitFor(() => screen.getByRole('button', { name: /Decline/i }));
      fireEvent.click(screen.getByRole('button', { name: /Decline/i }));

      await waitFor(() => {
        expect(api.emergency.updateMatchStatus).toHaveBeenCalledWith(55, 'REJECTED', false);
      });

      confirmSpy.mockRestore();
    });

    it('does NOT call updateMatchStatus if user cancels window.confirm', async () => {
      api.donor.getProfile.mockResolvedValue(MOCK_PROFILE);
      api.donor.getMyMatches.mockResolvedValue([MOCK_MATCH]);

      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

      renderDonorDashboard();

      await waitFor(() => screen.getByRole('button', { name: /Decline/i }));
      fireEvent.click(screen.getByRole('button', { name: /Decline/i }));

      await waitFor(() => {
        expect(api.emergency.updateMatchStatus).not.toHaveBeenCalled();
      });

      confirmSpy.mockRestore();
    });
  });
});
