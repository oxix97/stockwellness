import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const TICKER = optionalEnv('TICKER', '005930');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('stock_detail');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/stocks/${TICKER}`, {
    tags: { api: 'stock_detail', type: 'read' },
  });

  assertApiSuccess('stock detail', res);
  sleep(1);
}
