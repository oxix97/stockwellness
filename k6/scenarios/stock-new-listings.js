import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('stock_new_listings');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/stocks/new-listings`, {
    tags: { api: 'stock_new_listings', type: 'read' },
  });

  assertApiSuccess('stock new listings', res);
  sleep(1);
}
