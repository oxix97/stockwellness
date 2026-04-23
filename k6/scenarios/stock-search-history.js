import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('stock_search_history');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/stocks/search/history`, {
    headers: authHeaders(),
    tags: { api: 'stock_search_history', type: 'read' },
  });

  assertApiSuccess('stock search history', res);
  sleep(1);
}
