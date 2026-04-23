import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('member_notifications');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/members/me/notifications`, {
    headers: authHeaders(),
    tags: { api: 'member_notifications', type: 'read' },
  });

  assertApiSuccess('member notifications', res);
  sleep(1);
}
