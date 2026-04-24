import { readOptions, quickOptions } from '../helpers/options.js';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { jsonHeaders } from '../helpers/headers.js';

const BASE_URL = requireEnv('BASE_URL');
const REFRESH_TOKEN = requireEnv('REFRESH_TOKEN');

export const options = (__ENV.K6_MODE === 'quick' ? quickOptions : readOptions)('auth_reissue_burst');

export default function () {
  const url = `${BASE_URL}/api/v1/auth/reissue`;
  const payload = JSON.stringify({
    refreshToken: REFRESH_TOKEN,
  });

  const res = http.post(url, payload, {
    headers: jsonHeaders(),
    tags: {
      api: 'auth_reissue',
      type: 'auth',
    },
  });

  check(res, {
    'reissue status is 200': (r) => r.status === 200,
    'reissue returns token': (r) => {
      const body = r.json();
      return body && body.data && body.data.accessToken;
    },
  });

  sleep(0.3);
}
