import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const PORTFOLIO_ID = requireEnv('PORTFOLIO_ID');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('analysis_diversification');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/portfolios/${PORTFOLIO_ID}/analysis/diversification`, {
    headers: authHeaders(),
    tags: { api: 'analysis_diversification', type: 'read' },
  });

  assertApiSuccess('analysis diversification', res);
  sleep(1);
}
