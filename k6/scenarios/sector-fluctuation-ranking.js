import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const LIMIT = optionalEnv('LIMIT', '10');
const MARKET_TYPE = optionalEnv('MARKET_TYPE', '');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('sector_fluctuation_ranking');

export default function () {
  const marketTypeQuery = MARKET_TYPE ? `&marketType=${MARKET_TYPE}` : '';
  const res = http.get(`${BASE_URL}/api/v1/sectors/ranking/fluctuation?limit=${LIMIT}${marketTypeQuery}`, {
    tags: { api: 'sector_fluctuation_ranking', type: 'read' },
  });

  assertApiSuccess('sector fluctuation ranking', res);
  sleep(1);
}
