const API_BASE_URL = import.meta.env.VITE_API_BASE_URL !== undefined && import.meta.env.VITE_API_BASE_URL !== null ? import.meta.env.VITE_API_BASE_URL : '';

/**
 * Custom error class for API errors
 */
export class ApiError extends Error {
  constructor(message, status, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

/**
 * Centralized fetch wrapper with automatic JWT injection & error handling
 */
async function request(endpoint, options = {}) {
  const token = sessionStorage.getItem('lifelink_token');
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
  };

  const url = `${API_BASE_URL}${endpoint}`;

  try {
    const response = await fetch(url, config);

    // Handle 204 No Content
    if (response.status === 204) {
      return null;
    }

    let data;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }

    if (!response.ok) {
      // Handle Spring Security / Spring Boot error structures
      let errorMessage = 'An unexpected error occurred';
      if (typeof data === 'object' && data !== null) {
        if (data.message) {
          errorMessage = data.message;
        } else if (data.error && data.error !== 'Internal Server Error') {
          errorMessage = data.error;
        } else {
          // Check for validation errors map: { field: "message" }
          const errorList = Object.entries(data)
            .filter(([k]) => !['timestamp', 'status', 'error', 'path'].includes(k))
            .map(([_, v]) => String(v));
          if (errorList.length > 0) {
            errorMessage = errorList.join('. ');
          } else {
            errorMessage = data.error || 'Server error occurred. Please try again.';
          }
        }
      } else if (typeof data === 'string' && data.trim()) {
        errorMessage = data;
      }

      throw new ApiError(errorMessage, response.status, data);
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(error.message || 'Network error occurred. Please check backend connection.', 0);
  }
}

export const api = {
  // --- AUTHENTICATION ---
  auth: {
    login: (credentials) =>
      request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
      }),
    register: (userData) =>
      request('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(userData),
      }),
  },

  // --- DONOR ---
  donor: {
    getProfile: () => request('/api/donors/profile'),
    createProfile: (profile) =>
      request('/api/donors/profile', {
        method: 'POST',
        body: JSON.stringify(profile),
      }),
    updateProfile: (profile) =>
      request('/api/donors/profile', {
        method: 'PUT',
        body: JSON.stringify(profile),
      }),
    updateAvailability: (available) =>
      request('/api/donors/profile/availability', {
        method: 'PATCH',
        body: JSON.stringify({ available }),
      }),
    getMyMatches: () => request('/api/donors/matches'),
  },

  // --- HOSPITAL ---
  hospital: {
    getProfile: () => request('/api/hospitals/profile'),
    createProfile: (profile) =>
      request('/api/hospitals/profile', {
        method: 'POST',
        body: JSON.stringify(profile),
      }),
    updateProfile: (profile) =>
      request('/api/hospitals/profile', {
        method: 'PUT',
        body: JSON.stringify(profile),
      }),
  },

  // --- EMERGENCY REQUESTS ---
  emergency: {
    createRequest: (requestData) =>
      request('/api/emergency-requests', {
        method: 'POST',
        body: JSON.stringify(requestData),
      }),
    getMyRequests: (params = {}) => {
      const query = new URLSearchParams();
      if (params.status) query.append('status', params.status);
      if (params.bloodGroup) query.append('bloodGroup', params.bloodGroup);
      if (params.urgency) query.append('urgency', params.urgency);
      if (params.page !== undefined) query.append('page', params.page);
      if (params.size !== undefined) query.append('size', params.size);
      if (params.sort) query.append('sort', params.sort);
      
      const queryString = query.toString() ? `?${query.toString()}` : '';
      return request(`/api/emergency-requests${queryString}`);
    },
    updateStatus: (requestId, status) =>
      request(`/api/emergency-requests/${requestId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      }),
    findMatches: (requestId) =>
      request(`/api/emergency-requests/${requestId}/match`, {
        method: 'POST',
      }),
    getRequestMatches: (requestId) =>
      request(`/api/emergency-requests/${requestId}/matches`),
    updateMatchStatus: (matchId, status, consentToShareContact = false) =>
      request(`/api/emergency-requests/matches/${matchId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status, consentToShareContact }),
      }),
    getDonorContact: (requestId, matchId) =>
      request(`/api/emergency-requests/${requestId}/matches/${matchId}/contact`),
    completeMatch: (matchId) =>
      request(`/api/emergency-requests/matches/${matchId}/complete`, {
        method: 'PATCH',
      }),
  },

  // --- NOTIFICATIONS ---
  notifications: {
    getMyNotifications: () => request('/api/notifications'),
    markAsRead: (notificationId) =>
      request(`/api/notifications/${notificationId}/read`, {
        method: 'PATCH',
      }),
    getUnreadCount: () => request('/api/notifications/unread-count'),
  },

  // --- TEST / HEALTH ---
  test: {
    protected: () => request('/api/test/protected'),
    donorOnly: () => request('/api/test/donor-only'),
  }
};
