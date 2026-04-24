import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { assertApiSuccess } from '../helpers/checks.js';
import { readOptions, quickOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const SECTOR_CODE = optionalEnv('SECTOR_CODE', 'G25');

export const options = (__ENV.K6_MODE === "quick" ? quickOptions : readOptions)('sector_detail');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/sectors/${SECTOR_CODE}/detail`, {
    tags: { api: 'sector_detail', type: 'read' },
  });

  assertApiSuccess('sector detail', res);
  sleep(1);
}
