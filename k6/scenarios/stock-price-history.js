import http from 'k6/http';
import { sleep } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import {
  assertApiSuccess,
  createStockPriceHistoryValidator,
  resolveContractMode,
} from '../helpers/checks.js';
import { quickOptions, readOptions, strictReadOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const TICKER = optionalEnv('TICKER', '005930');
const PERIOD = optionalEnv('PERIOD', '1Y');
const FREQUENCY = optionalEnv('FREQUENCY', 'DAILY');
const INCLUDE_BENCHMARK = optionalEnv('INCLUDE_BENCHMARK', 'false');
const CONTRACT_MODE = resolveContractMode(__ENV.EOD_CONTRACT_MODE);
const IS_QUICK = (__ENV.K6_MODE || 'standard') === 'quick';
const EXPECTED = CONTRACT_MODE === 'fixture' ? JSON.parse(open('../fixtures/eod-read-expected.json')) : null;
const validateContract = CONTRACT_MODE === 'baseline'
  ? null
  : createStockPriceHistoryValidator(EXPECTED, {
      ticker: TICKER,
      period: PERIOD,
      frequency: FREQUENCY,
      includeBenchmark: INCLUDE_BENCHMARK,
      mode: CONTRACT_MODE,
      enforceBodyCache: !IS_QUICK,
    });

export const options = CONTRACT_MODE === 'fixture'
  ? strictReadOptions('stock_price_history')
  : (IS_QUICK ? quickOptions : readOptions)('stock_price_history');

export default function () {
  const res = http.get(
    `${BASE_URL}/api/v1/stocks/${TICKER}/prices/history?period=${PERIOD}&frequency=${FREQUENCY}&includeBenchmark=${INCLUDE_BENCHMARK}`,
    { tags: { api: 'stock_price_history', type: 'read' } },
  );

  if (CONTRACT_MODE === 'baseline') {
    assertApiSuccess('stock price history', res);
  } else {
    validateContract(res);
  }
  sleep(1);
}
