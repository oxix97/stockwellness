import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('member_me');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/members/me`, {
    headers: authHeaders(),
    tags: { api: 'member_me', type: 'read' },
  });

  assertApiSuccess('member me', res);
  sleep(1);
}
