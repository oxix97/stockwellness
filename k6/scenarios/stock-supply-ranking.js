import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const DIRECTION = optionalEnv('SUPPLY_DIRECTION', 'BUY');
const LIMIT = optionalEnv('LIMIT', '10');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('stock_supply_ranking');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/stocks/ranking/supply?direction=${DIRECTION}&limit=${LIMIT}`, {
    tags: { api: 'stock_supply_ranking', type: 'read' },
  });

  assertApiSuccess('stock supply ranking', res);
  sleep(1);
}
