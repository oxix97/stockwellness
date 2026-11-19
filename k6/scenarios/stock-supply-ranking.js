import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import {
  assertApiSuccess,
  createStockSupplyRankingValidator,
  resolveContractMode,
} from '../helpers/checks.js';
import { quickOptions, readOptions, strictReadOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const DIRECTION = optionalEnv('SUPPLY_DIRECTION', 'BUY');
const LIMIT = optionalEnv('LIMIT', '10');
const CONTRACT_MODE = resolveContractMode(__ENV.EOD_CONTRACT_MODE);
const IS_QUICK = (__ENV.K6_MODE || 'standard') === 'quick';
const EXPECTED = CONTRACT_MODE === 'fixture' ? JSON.parse(open('../fixtures/eod-read-expected.json')) : null;
const validateContract = CONTRACT_MODE === 'baseline'
  ? null
  : createStockSupplyRankingValidator(EXPECTED, {
      direction: DIRECTION,
      limit: Number(LIMIT),
      mode: CONTRACT_MODE,
      enforceBodyCache: !IS_QUICK,
    });

export const options = CONTRACT_MODE === 'fixture'
  ? strictReadOptions('stock_supply_ranking')
  : (IS_QUICK ? quickOptions : readOptions)('stock_supply_ranking');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/stocks/ranking/supply?direction=${DIRECTION}&limit=${LIMIT}`, {
    tags: { api: 'stock_supply_ranking', type: 'read' },
  });

  if (CONTRACT_MODE === 'baseline') {
    assertApiSuccess('stock supply ranking', res);
  } else {
    validateContract(res);
  }
  sleep(1);
}
