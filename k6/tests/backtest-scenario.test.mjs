import { test } from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';

let sent, failures, result;
globalThis.__ENV = {};
globalThis.__backtestTest = {
  post(url, body) { sent = { url, body: JSON.parse(body) }; return result; },
  count(n) { failures += n; },
  check(value, checks) { return Object.values(checks).every(fn => fn(value)); },
};
registerHooks({
  resolve(specifier, context, next) {
    return ['k6', 'k6/http', 'k6/metrics'].includes(specifier) ? { shortCircuit: true, url: `test:${specifier}` } : next(specifier, context);
  },
  load(url, context, next) {
    const sources = {
      'test:k6': 'export const check = (...a) => globalThis.__backtestTest.check(...a); export const sleep = () => {};',
      'test:k6/http': 'export default { post: (...a) => globalThis.__backtestTest.post(...a) };',
      'test:k6/metrics': 'export class Counter { add(n) { globalThis.__backtestTest.count(n); } }',
    };
    return sources[url] ? { shortCircuit: true, format: 'module', source: sources[url] } : next(url, context);
  },
});
let seq = 0;
async function scenario(env) {
  globalThis.__ENV = { BASE_URL: 'http://127.0.0.1', ACCESS_TOKEN: 'synthetic', PORTFOLIO_ID: '9003', ...env };
  failures = 0;
  return import(`../scenarios/backtest-heavy.js?test=${seq++}`);
}
test('baseline keeps request defaults and read and quick profiles', async () => {
  result = { status: 200, json: () => ({ data: {} }) };
  const heavy = await scenario({}); heavy.default();
  assert.deepEqual(sent.body, { strategy: 'LUMP_SUM', amount: 10000000, period: '1Y', dividendReinvested: true, rebalancingPeriod: 'MONTHLY', benchmarkTicker: 'SPY' });
  assert.equal(heavy.options.scenarios.backtest_heavy.executor, 'ramping-vus');
  assert.deepEqual(heavy.options.scenarios.backtest_heavy.stages, [{ duration: '1m', target: 10 }, { duration: '2m', target: 20 }, { duration: '1m', target: 30 }, { duration: '1m', target: 0 }]);
  for (const [mode, stages] of [['short', [['30s', 10], ['1m', 20], ['30s', 0]]], ['stress', [['2m', 20], ['4m', 40], ['2m', 60], ['2m', 0]]]]) {
    const mod = await scenario({ K6_MODE: mode });
    assert.deepEqual(mod.options.scenarios.backtest_heavy.stages, stages.map(([duration, target]) => ({ duration, target })));
  }
  const fixture = await scenario({ BACKTEST_CONTRACT_MODE: 'fixture' });
  assert.equal(fixture.options.scenarios.backtest_heavy.vus, 3);
  assert.equal(fixture.options.scenarios.backtest_heavy.duration, '3m');
  const quick = await scenario({ K6_MODE: 'quick', BACKTEST_STRATEGY: 'DCA' }); quick.default();
  assert.equal(sent.body.strategy, 'DCA');
  assert.equal(quick.options.scenarios.backtest_heavy.vus, 1);
  assert.equal(quick.options.scenarios.backtest_heavy.duration, '10s');
});
test('fixture mode rejects each invalid response and pins inputs', async () => {
  const mod = await scenario({ BACKTEST_CONTRACT_MODE: 'fixture', BACKTEST_STRATEGY: 'DCA', K6_MODE: 'quick' });
  result = { status: 200, body: '{"data":null}' }; mod.default();
  assert.equal(failures, 1);
  assert.deepEqual(sent.body, { strategy: 'DCA', amount: 1000, period: 'ALL', dividendReinvested: false, rebalancingPeriod: 'NONE', primaryBenchmark: 'KOSPI' });
  assert.deepEqual(mod.options.thresholds.backtest_contract_failures, ['count==0']);
  assert.deepEqual(mod.options.thresholds.checks, ['rate==1']);
  await assert.rejects(scenario({ BACKTEST_CONTRACT_MODE: 'fixture', BACKTEST_AMOUNT: '3' }), /Fixture requires/);
  await assert.rejects(scenario({ BACKTEST_CONTRACT_MODE: 'fixture', BACKTEST_AMOUNT: '0' }), /Fixture requires/);
  await assert.rejects(scenario({ BACKTEST_STRATEGY: 'other' }), /BACKTEST_STRATEGY/);
  await assert.rejects(scenario({ BACKTEST_CONTRACT_MODE: 'typo' }), /BACKTEST_CONTRACT_MODE/);
});
