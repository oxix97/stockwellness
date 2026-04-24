import { readOptions, quickOptions } from '../helpers/options.js';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';

const BASE_URL = requireEnv('BASE_URL');

export const options = (__ENV.K6_MODE === 'quick' ? quickOptions : readOptions)('smoke');

export default function () {
  const authTest = http.get(`${BASE_URL}/api/v1/auth/test`, {
    tags: {
      api: 'auth_test',
      type: 'smoke',
    },
  });

  check(authTest, {
    'auth test status is 200': (r) => r.status === 200,
  });

  const health = http.get(`${BASE_URL}/actuator/health`, {
    tags: {
      api: 'actuator_health',
      type: 'smoke',
    },
  });

  check(health, {
    'health status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
