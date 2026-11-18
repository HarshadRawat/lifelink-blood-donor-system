import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Task 10: Emergency Blood Request Creation Load Test
 * Tests hospital emergency request creation and validation under concurrency.
 */
export const options = {
  stages: [
    { duration: '15s', target: 20 },
    { duration: '30s', target: 50 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<300'],
    http_req_failed: ['rate<0.02'],
  },
};

const BASE_URL = __ENV.BASE || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

const bloodGroups = ['O+', 'A+', 'B+', 'AB+', 'O-', 'A-', 'B-', 'AB-'];
const urgencies = ['CRITICAL', 'HIGH', 'MEDIUM'];

export default function () {
  const bloodGroup = bloodGroups[Math.floor(Math.random() * bloodGroups.length)];
  const urgency = urgencies[Math.floor(Math.random() * urgencies.length)];
  const unitsRequired = Math.floor(Math.random() * 4) + 1;

  const payload = JSON.stringify({
    bloodGroup: bloodGroup,
    unitsRequired: unitsRequired,
    urgency: urgency,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-ID': `k6-request-${__VU}-${__ITER}`,
    },
  };

  if (TOKEN) {
    params.headers['Authorization'] = `Bearer ${TOKEN}`;
  }

  const res = http.post(`${BASE_URL}/api/emergency-requests`, payload, params);

  check(res, {
    'status is 200 or 401/409': (r) => r.status === 200 || r.status === 401 || r.status === 409,
    'duration within SLA': (r) => r.timings.duration < 300,
  });

  sleep(1);
}
