import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('stock_popular_search');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/stocks/popular-search`, {
    tags: { api: 'stock_popular_search', type: 'read' },
  });

  assertApiSuccess('stock popular search', res);
  sleep(1);
}
