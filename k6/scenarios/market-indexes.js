import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import {
  assertApiSuccess,
  createMarketIndexesValidator,
  resolveContractMode,
} from '../helpers/checks.js';
import { quickOptions, readOptions, strictReadOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const CONTRACT_MODE = resolveContractMode(__ENV.EOD_CONTRACT_MODE);
const IS_QUICK = (__ENV.K6_MODE || 'standard') === 'quick';
const EXPECTED = CONTRACT_MODE === 'fixture' ? JSON.parse(open('../fixtures/eod-read-expected.json')) : null;
const validateContract = CONTRACT_MODE === 'baseline'
  ? null
  : createMarketIndexesValidator(EXPECTED, { mode: CONTRACT_MODE, enforceBodyCache: !IS_QUICK });

export const options = CONTRACT_MODE === 'fixture'
  ? strictReadOptions('market_indexes')
  : (IS_QUICK ? quickOptions : readOptions)('market_indexes');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/market/indexes`, {
    tags: { api: 'market_indexes', type: 'read' },
  });

  if (CONTRACT_MODE === 'baseline') {
    assertApiSuccess('market indexes', res);
  } else {
    validateContract(res);
  }
  sleep(1);
}
