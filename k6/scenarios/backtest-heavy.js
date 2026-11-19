import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { requireEnv, optionalEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';
import { quickOptions, readOptions, heavyReadOptions } from '../helpers/options.js';
import { backtestFixtureFailure } from '../helpers/backtest-contract.js';

const BASE_URL = requireEnv('BASE_URL');
const PORTFOLIO_ID = requireEnv('PORTFOLIO_ID');
const STRATEGY = optionalEnv('BACKTEST_STRATEGY', 'LUMP_SUM');
const MODE = optionalEnv('BACKTEST_CONTRACT_MODE', 'baseline');
if (!['LUMP_SUM', 'DCA'].includes(STRATEGY)) throw new Error('BACKTEST_STRATEGY must be LUMP_SUM or DCA');
if (!['baseline', 'fixture'].includes(MODE)) throw new Error('BACKTEST_CONTRACT_MODE must be baseline or fixture');
const fixture = MODE === 'fixture';
const amount = Number(optionalEnv('BACKTEST_AMOUNT', fixture ? '1000' : '10000000'));
const period = optionalEnv('BACKTEST_PERIOD', fixture ? 'ALL' : '1Y');
const benchmark = optionalEnv('BENCHMARK_TICKER', fixture ? 'KOSPI' : 'SPY');
if (fixture && (PORTFOLIO_ID !== '9003' || amount !== 1000 || period !== 'ALL' || benchmark !== 'KOSPI')) {
  throw new Error('Fixture requires portfolio 9003, amount 1000, ALL period and KOSPI');
}
const contractFailures = new Counter('backtest_contract_failures');
const base = (__ENV.K6_MODE === 'quick' ? quickOptions : fixture ? heavyReadOptions : readOptions)('backtest_heavy');
export const options = fixture ? {
  ...base,
  thresholds: { ...base.thresholds, http_req_failed: ['rate==0'], checks: ['rate==1'], backtest_contract_failures: ['count==0'] },
} : base;

export default function () {
  const payload = {
    strategy: STRATEGY,
    amount,
    period,
    dividendReinvested: !fixture,
    rebalancingPeriod: fixture ? 'NONE' : 'MONTHLY',
  };
  if (fixture) payload.primaryBenchmark = benchmark;
  else payload.benchmarkTicker = benchmark;
  const res = http.post(`${BASE_URL}/api/v1/portfolios/${PORTFOLIO_ID}/analysis/backtest`, JSON.stringify(payload), {
    headers: authHeaders(), tags: { api: 'analysis_backtest', type: 'heavy', strategy: STRATEGY }, timeout: '15s',
  });
  if (fixture) {
    const failure = backtestFixtureFailure(res, STRATEGY);
    contractFailures.add(failure ? 1 : 0);
    check(res, { 'backtest fixture contract': () => failure === null });
    if (failure) console.error(`backtest ${STRATEGY} contract failed: ${failure}`);
  } else {
    check(res, {
      'backtest status is 200': (r) => r.status === 200,
      'backtest has result': (r) => {
        try { return Boolean(r.json()?.data); } catch (_) { return false; }
      },
    });
  }
  sleep(1);
}
