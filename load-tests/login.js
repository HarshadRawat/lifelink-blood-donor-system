import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Task 10: Authentication & Login Load Test
 * Evaluates BCrypt password matching, JWT generation, and refresh token rotation under load.
 */
export const options = {
  stages: [
    { duration: '15s', target: 20 },
    { duration: '30s', target: 50 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<400'], // BCrypt hashing cost is deliberately CPU-intensive (~80ms)
    http_req_failed: ['rate<0.02'],
  },
};

const BASE_URL = __ENV.BASE || 'http://localhost:8080';
const EMAIL = __ENV.LOGIN_EMAIL || 'admin@lifelink.com';
const PASSWORD = __ENV.LOGIN_PASSWORD || 'Admin123';

export default function () {
  const payload = JSON.stringify({
    email: EMAIL,
    password: PASSWORD,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-ID': `k6-login-${__VU}-${__ITER}`,
    },
  };

  const res = http.post(`${BASE_URL}/api/auth/login`, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'jwt token returned': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.token !== undefined && body.token.length > 20;
      } catch (e) {
        return false;
      }
    },
    'refresh token returned': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.refreshToken !== undefined && body.refreshToken.length > 20;
      } catch (e) {
        return false;
      }
    },
  });

  sleep(1);
}
