import http from 'k6/http';
import { check, group, sleep } from 'k6';

/**
 * Task 10: Complete Hospital Lifecycle Scenario Load Test
 * Executes:
 *   1. User Login (acquires access token)
 *   2. Fetch Hospital Profile
 *   3. Create Emergency Request
 *   4. Query Candidate Matches
 */
export const options = {
  stages: [
    { duration: '10s', target: 10 },
    { duration: '30s', target: 30 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    'http_req_duration{scenario:default}': ['p(95)<350'],
    http_req_failed: ['rate<0.02'],
  },
};

const BASE_URL = __ENV.BASE || 'http://localhost:8080';
const EMAIL = __ENV.LOGIN_EMAIL || 'hospital@lifelink.com';
const PASSWORD = __ENV.LOGIN_PASSWORD;
if (!PASSWORD) {
  throw new Error('LOGIN_PASSWORD environment variable is required (no default password permitted)');
}

export default function () {
  let token = '';

  group('1. Authentication Flow', function () {
    const loginPayload = JSON.stringify({ email: EMAIL, password: PASSWORD });
    const res = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
      headers: {
        'Content-Type': 'application/json',
        'X-Correlation-ID': `lifecycle-auth-${__VU}-${__ITER}`,
      },
    });

    check(res, { 'login status 200': (r) => r.status === 200 });

    try {
      const data = JSON.parse(res.body);
      token = data.token;
    } catch (e) {
      // Ignored for error branch
    }
  });

  if (!token) {
    sleep(1);
    return;
  }

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-Correlation-ID': `lifecycle-api-${__VU}-${__ITER}`,
  };

  group('2. Emergency Request Flow', function () {
    const payload = JSON.stringify({
      bloodGroup: 'O+',
      unitsRequired: 2,
      urgency: 'HIGH',
    });

    const res = http.post(`${BASE_URL}/api/emergency-requests`, payload, {
      headers: authHeaders,
    });

    check(res, {
      'request created or rejected with business rule': (r) =>
        r.status === 200 || r.status === 409,
    });
  });

  group('3. Donor Match Inspection', function () {
    const res = http.get(`${BASE_URL}/api/donors/matches`, {
      headers: authHeaders,
    });

    check(res, { 'matches returned 200': (r) => r.status === 200 });
  });

  sleep(1);
}
