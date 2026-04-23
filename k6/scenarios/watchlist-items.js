import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const GROUP_ID = requireEnv('GROUP_ID');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('watchlist_items');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/watchlist/groups/${GROUP_ID}/items`, {
    headers: authHeaders(),
    tags: { api: 'watchlist_items', type: 'read' },
  });

  assertApiSuccess('watchlist items', res);
  sleep(1);
}
