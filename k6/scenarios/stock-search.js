import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const SEARCH_KEYWORD = optionalEnv('SEARCH_KEYWORD', '삼성');
const PAGE = optionalEnv('PAGE', '0');
const SIZE = optionalEnv('SIZE', '20');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('stock_search');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/stocks/search?keyword=${encodeURIComponent(SEARCH_KEYWORD)}&page=${PAGE}&size=${SIZE}`, {
    tags: { api: 'stock_search', type: 'read' },
  });

  assertApiSuccess('stock search', res);
  sleep(1);
}
