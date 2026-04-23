import { readOptions, quickOptions } from '../helpers/options.js';
import http from 'k6/http';
import { check } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';

const BASE_URL = requireEnv('BASE_URL');
const GROUP_NAME = optionalEnv('GROUP_NAME', 'k6-live-group');

export const options = (__ENV.K6_MODE === 'quick' ? quickOptions : readOptions)('watchlist_group_create');

export default function () {
  const payload = JSON.stringify({ name: GROUP_NAME });

  const res = http.post(`${BASE_URL}/api/v1/watchlist/groups`, payload, {
    headers: authHeaders(),
    tags: { api: 'watchlist_group_create', type: 'write' },
  });

  check(res, {
    'watchlist group create status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'watchlist group create has id': (r) => {
      const body = r.json();
      return body && typeof body.data === 'number';
    },
  });

  const body = res.json();
  console.log(JSON.stringify({ groupId: body.data }));
}
