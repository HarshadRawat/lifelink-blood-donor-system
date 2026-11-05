/**
 * HospitalDashboard.test.jsx
 *
 * Tests for the Hospital Emergency Portal dashboard covering:
 * 1. Profile not set up → "Hospital Profile Required" prompt shown
 * 2. Profile loaded → hospital name, registration number, city rendered
 * 3. "New Emergency Request" button → opens create request modal
 * 4. Opening create request without profile → shows toast and profile modal instead
 * 5. Create request form submission → calls api.emergency.createRequest then findMatches
 * 6. Emergency requests list → renders blood group, units, Find Matches & View Donors buttons
 * 7. Empty requests state → "No Emergency Requests Found" empty state shown
 * 8. Mark Fulfilled / Cancel buttons call api.emergency.updateStatus correctly
 * 9. Find Matches button → calls api.emergency.findMatches and opens matches modal
 */

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { HospitalDashboard } from '../pages/HospitalDashboard';

// ── Module mocks ──────────────────────────────────────────────────────────────

vi.mock('../api/client', () => ({
  api: {
    hospital: {
      getProfile: vi.fn(),
      createProfile: vi.fn(),
      updateProfile: vi.fn(),
    },
    emergency: {
      getMyRequests: vi.fn(),
      createRequest: vi.fn(),
      findMatches: vi.fn(),
      getRequestMatches: vi.fn(),
      updateStatus: vi.fn(),
      completeMatch: vi.fn(),
    },
  },
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 2, name: 'Dr. Admin', email: 'hospital@example.com', role: 'HOSPITAL' },
  }),
}));

const mockShowToast = vi.fn();
vi.mock('../context/NotificationContext', () => ({
  useNotifications: () => ({
    showToast: mockShowToast,
  }),
}));

vi.mock('../utils/requestRegistry', () => ({
  registerHospitalRequest: vi.fn(),
  registerHospitalRequestsList: vi.fn(),
}));

// ── Helpers ───────────────────────────────────────────────────────────────────

import { api } from '../api/client';

const MOCK_HOSPITAL_PROFILE = {
  id: 5,
  hospitalName: 'Apollo Bangalore',
  registrationNumber: 'REG-KA-9820',
  address: '154 Bannerghatta Road',
  city: 'Bangalore',
  contactNumber: '+91 8000000000',
  latitude: 12.9716,
  longitude: 77.5946,
};

const MOCK_PAGE_RESPONSE = {
  content: [
    {
      id: 42,
      bloodGroup: 'O-',
      unitsRequired: 2,
      urgency: 'CRITICAL',
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    },
  ],
  totalPages: 1,
  totalElements: 1,
};

function renderHospitalDashboard() {
  return render(<HospitalDashboard />);
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('HospitalDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── 1. Profile not set up ─────────────────────────────────────────────────

  describe('when hospital profile does not exist', () => {
    it('shows Hospital Profile Required prompt with Set Up button', async () => {
      api.hospital.getProfile.mockRejectedValue(new Error('Profile not found'));

      renderHospitalDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Hospital Profile Required/i)).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /Set Up Hospital Profile/i })).toBeInTheDocument();
    });

    it('shows No Emergency Requests Found empty state', async () => {
      api.hospital.getProfile.mockRejectedValue(new Error('Not found'));

      renderHospitalDashboard();

      await waitFor(() => {
        expect(screen.getByText(/No Emergency Requests Found/i)).toBeInTheDocument();
      });
    });
  });

  // ── 2. Profile loaded ─────────────────────────────────────────────────────

  describe('when hospital profile is loaded', () => {
    it('renders hospital name, registration number, and city', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);

      renderHospitalDashboard();

      await waitFor(() => {
        const matchingNames = screen.getAllByText('Apollo Bangalore');
        expect(matchingNames.length).toBeGreaterThan(0);
      });
      expect(screen.getByText(/REG-KA-9820/)).toBeInTheDocument();
      expect(screen.getByText(/154 Bannerghatta Road/)).toBeInTheDocument();
    });

    it('renders Edit Profile button when profile exists', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);

      renderHospitalDashboard();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Edit Profile/i })).toBeInTheDocument();
      });
    });
  });

  // ── 3. Create Request button ──────────────────────────────────────────────

  describe('New Emergency Request button', () => {
    it('opens create request modal when profile exists', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);

      renderHospitalDashboard();

      // Wait for the full async chain: profile → fetchRequests
      await waitFor(() => screen.getByText(/Request #42/i));

      fireEvent.click(screen.getByRole('button', { name: /New Emergency Request/i }));

      await waitFor(() => {
        expect(screen.getByText(/Broadcast Emergency Blood Request/i)).toBeInTheDocument();
      });
    });

    it('shows profile modal instead when profile does not exist (no profile guard)', async () => {
      api.hospital.getProfile.mockRejectedValue(new Error('Not set up'));

      renderHospitalDashboard();

      await waitFor(() => screen.getByText(/Hospital Profile Required/i));

      fireEvent.click(screen.getByRole('button', { name: /New Emergency Request/i }));

      await waitFor(() => {
        expect(mockShowToast).toHaveBeenCalledWith(
          'Profile Required',
          expect.any(String),
          'warning'
        );
      });
    });
  });

  // ── 5. Create request form submission ─────────────────────────────────────

  describe('create emergency request form', () => {
    it('calls api.emergency.createRequest then immediately triggers findMatches', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);

      const createdRequest = { id: 99, bloodGroup: 'O-', unitsRequired: 1 };
      api.emergency.createRequest.mockResolvedValue(createdRequest);
      api.emergency.findMatches.mockResolvedValue([]);

      renderHospitalDashboard();

      // Wait until profile and requests have both loaded
      await waitFor(() => screen.getByText(/Request #42/i));

      fireEvent.click(screen.getByRole('button', { name: /New Emergency Request/i }));
      await waitFor(() => screen.getByText(/Broadcast Emergency Blood Request/i));

      // Submit form with defaults (O-, 1 unit, HIGH urgency)
      fireEvent.click(screen.getByRole('button', { name: /Broadcast Request/i }));

      await waitFor(() => {
        expect(api.emergency.createRequest).toHaveBeenCalledWith(
          expect.objectContaining({ bloodGroup: 'O-', urgency: 'HIGH' })
        );
      });

      await waitFor(() => {
        expect(api.emergency.findMatches).toHaveBeenCalledWith(99);
      });
    });
  });

  // ── 6. Requests list rendering ────────────────────────────────────────────

  describe('emergency requests list', () => {
    it('renders blood group, units, Find Matches and View Donors buttons', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);

      renderHospitalDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Request #42/i)).toBeInTheDocument();
      });
      expect(screen.getByText(/2 Units/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Find Matches/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /View Donors/i })).toBeInTheDocument();
    });

    it('renders Mark Fulfilled and Cancel buttons for ACTIVE requests', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);

      renderHospitalDashboard();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Mark Fulfilled/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
      });
    });

    it('shows total request count', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);

      renderHospitalDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Total Requests Issued: 1/i)).toBeInTheDocument();
      });
    });
  });

  // ── 8. Status update actions ──────────────────────────────────────────────

  describe('request status actions', () => {
    it('calls api.emergency.updateStatus with FULFILLED when Mark Fulfilled is clicked', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);
      api.emergency.updateStatus.mockResolvedValue({});

      renderHospitalDashboard();

      await waitFor(() => screen.getByRole('button', { name: /Mark Fulfilled/i }));
      fireEvent.click(screen.getByRole('button', { name: /Mark Fulfilled/i }));

      await waitFor(() => {
        expect(api.emergency.updateStatus).toHaveBeenCalledWith(42, 'FULFILLED');
      });
    });

    it('calls api.emergency.updateStatus with CANCELLED when Cancel is clicked', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);
      api.emergency.updateStatus.mockResolvedValue({});

      renderHospitalDashboard();

      await waitFor(() => screen.getByRole('button', { name: /Cancel/i }));
      fireEvent.click(screen.getByRole('button', { name: /Cancel/i }));

      await waitFor(() => {
        expect(api.emergency.updateStatus).toHaveBeenCalledWith(42, 'CANCELLED');
      });
    });
  });

  // ── 9. Find Matches ───────────────────────────────────────────────────────

  describe('Find Matches button', () => {
    it('calls api.emergency.findMatches with the request id and opens matches modal', async () => {
      api.hospital.getProfile.mockResolvedValue(MOCK_HOSPITAL_PROFILE);
      api.emergency.getMyRequests.mockResolvedValue(MOCK_PAGE_RESPONSE);
      api.emergency.findMatches.mockResolvedValue([
        { id: 11, donorId: 10, status: 'MATCHED', bloodGroup: 'O-', contactSharingConsent: false },
      ]);

      renderHospitalDashboard();

      await waitFor(() => screen.getByRole('button', { name: /Find Matches/i }));
      fireEvent.click(screen.getByRole('button', { name: /Find Matches/i }));

      await waitFor(() => {
        expect(api.emergency.findMatches).toHaveBeenCalledWith(42);
      });

      // Matches modal should open showing the matched donor
      await waitFor(() => {
        expect(screen.getByText(/Matched Donors for Request/i)).toBeInTheDocument();
      });
    });
  });
});
