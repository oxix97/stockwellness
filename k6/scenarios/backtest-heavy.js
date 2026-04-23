import { readOptions, quickOptions } from '../helpers/options.js';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { requireEnv, optionalEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';

const BASE_URL = requireEnv('BASE_URL');
const PORTFOLIO_ID = requireEnv('PORTFOLIO_ID');
const BACKTEST_AMOUNT = Number(optionalEnv('BACKTEST_AMOUNT', '10000000'));
const BACKTEST_PERIOD = optionalEnv('BACKTEST_PERIOD', '1Y');
const BENCHMARK_TICKER = optionalEnv('BENCHMARK_TICKER', 'SPY');

export const options = (__ENV.K6_MODE === 'quick' ? quickOptions : readOptions)('backtest_heavy');

export default function () {
  const url = `${BASE_URL}/api/v1/portfolios/${PORTFOLIO_ID}/analysis/backtest`;

  const payload = JSON.stringify({
    strategy: 'LUMP_SUM',
    amount: BACKTEST_AMOUNT,
    benchmarkTicker: BENCHMARK_TICKER,
    period: BACKTEST_PERIOD,
    dividendReinvested: true,
    rebalancingPeriod: 'MONTHLY',
  });

  const res = http.post(url, payload, {
    headers: authHeaders(),
    tags: {
      api: 'analysis_backtest',
      type: 'heavy',
    },
    timeout: '15s',
  });

  check(res, {
    'backtest status is 200': (r) => r.status === 200,
    'backtest has result': (r) => {
      const body = r.json();
      return body && body.data;
    },
  });

  sleep(1);
}
