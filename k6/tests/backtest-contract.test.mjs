import { test } from 'node:test';
import assert from 'node:assert/strict';
import { backtestFixtureFailure } from '../helpers/backtest-contract.js';

const dailyResults = [
  { date: '2025-01-02', totalValue: 1000, totalInvested: 1000, returnRate: 0, benchmarkReturnRate: 0, benchmarkReturnRates: { KOSPI: 0 } },
  { date: '2025-01-15', totalValue: 1100, totalInvested: 1000, returnRate: 10, benchmarkReturnRate: 5, benchmarkReturnRates: { KOSPI: 5 } },
  { date: '2026-01-02', totalValue: 1100, totalInvested: 1000, returnRate: 0, benchmarkReturnRate: 10, benchmarkReturnRates: { KOSPI: 10 } },
];
const lump = { primaryBenchmark: 'KOSPI', calculationMethod: 'LUMP_SUM_CAGR_TWR', dailyResults, totalReturnRate: 10, timeWeightedReturnRate: 10, cagr: 10, xirr: null };
const dca = { ...lump, calculationMethod: 'DCA_XIRR_TWR', cagr: null, xirr: 10, totalReturnRate: 10,
  dailyResults: [...dailyResults.slice(0, 2), { ...dailyResults[2], totalValue: 2100, totalInvested: 2000 }] };
const response = (data, status = 200) => ({ status, body: JSON.stringify({ data }) });

test('accepts independently calculated lump sum and DCA results', () => {
  assert.equal(backtestFixtureFailure(response(lump), 'LUMP_SUM'), null);
  assert.equal(backtestFixtureFailure(response(dca), 'DCA'), null);
});
test('rejects one error, empty or corrupted response for either strategy', () => {
  for (const [strategy, data] of [['LUMP_SUM', lump], ['DCA', dca]]) {
    for (const res of [response(data, 500), response(null), { status: 200, body: '{' },
      response({ ...data, primaryBenchmark: 'SP500' }), response({ ...data, cagr: 99 }),
      response({ ...data, xirr: 99 }), response({ ...data, dailyResults: [] }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 2 ? { ...d, totalValue: d.totalValue + 1 } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 2 ? { ...d, date: '2026-01-03' } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 2 ? { ...d, benchmarkReturnRate: 9 } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 0 ? { ...d, returnRate: null } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 1 ? { ...d, returnRate: undefined } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 2 ? { ...d, returnRate: 1 } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 1 ? { ...d, benchmarkReturnRates: undefined } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 1 ? { ...d, benchmarkReturnRates: null } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 1 ? { ...d, benchmarkReturnRates: {} } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 1 ? { ...d, benchmarkReturnRates: { KOSPI: null } } : d) }),
      response({ ...data, dailyResults: data.dailyResults.map((d, i) => i === 1 ? { ...d, benchmarkReturnRates: { KOSPI: 6 } } : d) }),
      response({ ...data, totalReturnRate: 6 })]) {
      assert.notEqual(backtestFixtureFailure(res, strategy), null);
    }
  }
});
