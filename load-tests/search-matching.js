import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Task 10: Geolocation Matching Load Test
 * Evaluates candidate matching latency under 50 concurrent virtual users.
 * Thresholds:
 *   - 95th percentile latency must be under 300ms (p(95) < 300)
 *   - Failure rate must be under 1% (rate < 0.01)
 */
export const options = {
  vus: 50,
  duration: '60s',
  thresholds: {
    http_req_duration: ['p(95)<300'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

export default function () {
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-ID': `k6-search-${__VU}-${__ITER}`,
    },
  };

  if (TOKEN) {
    params.headers['Authorization'] = `Bearer ${TOKEN}`;
  }

  // Request matches endpoint for emergency request or candidate lookup
  const res = http.get(`${BASE_URL}/api/donors/matches`, params);

  check(res, {
    'status is 200 or 401': (r) => r.status === 200 || r.status === 401,
    'response received within 300ms': (r) => r.timings.duration < 300,
  });

  sleep(1);
}
