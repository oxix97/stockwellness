import { readOptions, quickOptions } from '../helpers/options.js';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';

const BASE_URL = requireEnv('BASE_URL');
const PORTFOLIO_ID = requireEnv('PORTFOLIO_ID');

export const options = (__ENV.K6_MODE === 'quick' ? quickOptions : readOptions)('health_read');

export default function () {
  const url = `${BASE_URL}/api/v1/portfolios/${PORTFOLIO_ID}/health`;

  const res = http.get(url, {
    headers: authHeaders(),
    tags: {
      api: 'portfolio_health',
      type: 'read',
    },
  });

  check(res, {
    'health status is 200': (r) => r.status === 200,
    'health has data': (r) => {
      const body = r.json();
      return body && body.data;
    },
  });

  sleep(1);
}
