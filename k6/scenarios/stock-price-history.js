import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const TICKER = optionalEnv('TICKER', '005930');
const PERIOD = optionalEnv('PERIOD', '1Y');
const FREQUENCY = optionalEnv('FREQUENCY', 'DAILY');
const INCLUDE_BENCHMARK = optionalEnv('INCLUDE_BENCHMARK', 'false');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('stock_price_history');

export default function () {
  const res = http.get(
    `${BASE_URL}/api/v1/stocks/${TICKER}/prices/history?period=${PERIOD}&frequency=${FREQUENCY}&includeBenchmark=${INCLUDE_BENCHMARK}`,
    { tags: { api: 'stock_price_history', type: 'read' } },
  );

  assertApiSuccess('stock price history', res);
  sleep(1);
}
