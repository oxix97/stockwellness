import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { registerHooks } from 'node:module';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const K6_ROOT = path.resolve(__dirname, '..');
const EXPECTED_PATH = path.join(K6_ROOT, 'fixtures/eod-read-expected.json');
const EXPECTED = JSON.parse(fs.readFileSync(EXPECTED_PATH, 'utf8'));

let checkPassCount = 0;
let checkFailCount = 0;
let checkResults = [];
let counterTotals = new Map();
let lastHttpGet = null;
let mockHttpResponse = { status: 200, body: '{}', json() { return {}; } };

globalThis.__ENV = {};
globalThis.open = (relPath) => {
  const resolved = path.resolve(K6_ROOT, 'scenarios', relPath);
  return fs.readFileSync(resolved, 'utf8');
};

globalThis.__k6TestHarness = {
  recordCheck(name, ok) {
    if (ok) checkPassCount += 1;
    else checkFailCount += 1;
    checkResults.push({ name, ok });
  },
  recordCounter(name, n) {
    counterTotals.set(name, (counterTotals.get(name) || 0) + n);
  },
  httpGet(url, params) {
    lastHttpGet = { url, params };
    return mockHttpResponse;
  },
};

registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier === 'k6' || specifier === 'k6/http' || specifier === 'k6/metrics') {
      return { shortCircuit: true, url: `k6test:${specifier}` };
    }
    return nextResolve(specifier, context);
  },
  load(url, context, nextLoad) {
    if (url === 'k6test:k6') {
      return {
        shortCircuit: true,
        format: 'module',
        source: `
          export function check(val, sets) {
            let allPass = true;
            for (const [name, fn] of Object.entries(sets)) {
              const ok = Boolean(fn(val));
              globalThis.__k6TestHarness.recordCheck(name, ok);
              if (!ok) allPass = false;
            }
            return allPass;
          }
          export function sleep() {}
        `,
      };
    }
    if (url === 'k6test:k6/metrics') {
      return {
        shortCircuit: true,
        format: 'module',
        source: `
          export class Counter {
            constructor(name) { this.name = name; }
            add(n = 1) { globalThis.__k6TestHarness.recordCounter(this.name, n); }
          }
        `,
      };
    }
    if (url === 'k6test:k6/http') {
      return {
        shortCircuit: true,
        format: 'module',
        source: `
          export default {
            get(url, params) {
              return globalThis.__k6TestHarness.httpGet(url, params);
            },
          };
        `,
      };
    }
    return nextLoad(url, context);
  },
});

const checks = await import(pathToFileURL(path.join(K6_ROOT, 'helpers/checks.js')).href);
const options = await import(pathToFileURL(path.join(K6_ROOT, 'helpers/options.js')).href);

let importNonce = 0;
async function importScenario(name, env) {
  importNonce += 1;
  globalThis.__ENV = { ...env };
  const scenarioUrl = `${pathToFileURL(path.join(K6_ROOT, 'scenarios', name)).href}?v=${importNonce}`;
  return import(scenarioUrl);
}

function resetHarness(response = null) {
  checkPassCount = 0;
  checkFailCount = 0;
  checkResults = [];
  counterTotals = new Map();
  lastHttpGet = null;
  if (response) {
    mockHttpResponse = response;
  }
}

function makeRes(payload, status = 200) {
  const body = typeof payload === 'string' ? payload : JSON.stringify(payload);
  return {
    status,
    body,
    json() {
      return JSON.parse(this.body);
    },
  };
}

function validFixtureMarketIndexesPayload() {
  return {
    success: true,
    data: {
      indexes: EXPECTED.marketIndexes.indexes.map((idx) => ({
        ticker: idx.ticker,
        name: idx.name,
        currentPrice: idx.currentPrice,
        fluctuationRate: idx.fluctuationRate,
        fluctuationAmount: 999.99, // stubbed zero field must be ignored in fixture mode
        history: idx.history.map((pt) => ({ ...pt })),
      })),
      weather: { ...EXPECTED.marketIndexes.expectedWeather },
    },
  };
}

function validFixtureSupplyPayload(direction = 'BUY') {
  const dirFixture = EXPECTED.supplyRanking[direction];
  return {
    success: true,
    data: {
      requestedDate: null,
      effectiveDate: EXPECTED.supplyRanking.effectiveDate,
      institutionItems: dirFixture.institutionItems.map((item) => ({ ...item })),
      foreignItems: dirFixture.foreignItems.map((item) => ({ ...item })),
    },
  };
}

function validFixturePriceHistoryPayload(includeBenchmark = true) {
  const stock = EXPECTED.priceHistory.stocks['990101'];
  const daily = stock.DAILY;
  return {
    success: true,
    data: {
      ticker: stock.ticker,
      currency: stock.currency,
      stockName: stock.stockName,
      benchmarkName: stock.benchmarkName,
      prices: daily.prices.map((p) => ({ ...p })),
      benchmarks: includeBenchmark ? daily.benchmarks.map((b) => ({ ...b })) : [],
    },
  };
}

function validFixtureValuationPayload() {
  const exp = EXPECTED.valuation.portfolios['9001'];
  return {
    success: true,
    data: {
      totalPurchaseAmount: exp.totalPurchaseAmount,
      currentTotalValue: exp.currentTotalValue,
      totalProfitLoss: exp.totalProfitLoss,
      totalReturnRate: exp.totalReturnRate,
      dailyProfitLoss: exp.dailyProfitLoss,
      dailyReturnRate: exp.dailyReturnRate,
      cagr: 0,
      volatility: 0,
      alpha: 0,
      mdd: 0,
      sharpeRatio: 0,
      beta: 0,
      totalInstitutionalNetBuying: 12345, // stubbed zero field must be ignored
      totalForeignNetBuying: -6789,
      totalPersonNetBuying: 42,
      valuationStatus: exp.valuationStatus,
      asOfDate: exp.asOfDate,
      missingSymbols: [],
    },
  };
}

describe('EOD k6 contract mode & options backward compatibility', () => {
  it('keeps readOptions baseline behavior unchanged even under K6_MODE=quick', () => {
    globalThis.__ENV = { K6_MODE: 'quick' };
    const opt = options.readOptions('market_indexes');
    assert.equal(opt.scenarios.market_indexes.executor, 'ramping-vus');
    assert.deepEqual(opt.thresholds.checks, ['rate>0.99']);
    assert.deepEqual(opt.thresholds.http_req_failed, ['rate<0.01']);
    assert.equal(opt.thresholds.eod_contract_failures, undefined);

    const strictQuick = options.strictReadOptions('market_indexes');
    assert.equal(strictQuick.scenarios.market_indexes.executor, 'constant-vus');
    assert.deepEqual(strictQuick.thresholds.http_req_failed, ['rate==0']);
    assert.deepEqual(strictQuick.thresholds.checks, ['rate==1']);
    assert.deepEqual(strictQuick.thresholds.eod_contract_failures, ['count==0']);
  });

  it('defaults scenarios without EOD_CONTRACT_MODE to baseline mode and baseline thresholds', async () => {
    const scenarios = [
      { file: 'market-indexes.js', key: 'market_indexes', extraEnv: {} },
      { file: 'stock-supply-ranking.js', key: 'stock_supply_ranking', extraEnv: {} },
      { file: 'stock-price-history.js', key: 'stock_price_history', extraEnv: {} },
      {
        file: 'analysis-valuation.js',
        key: 'analysis_valuation',
        extraEnv: { PORTFOLIO_ID: '18', ACCESS_TOKEN: 'jwt-token' },
      },
    ];

    for (const s of scenarios) {
      const mod = await importScenario(s.file, {
        BASE_URL: 'http://127.0.0.1:18080',
        ...s.extraEnv,
      });
      assert.equal(mod.options.scenarios[s.key].executor, 'ramping-vus');
      assert.deepEqual(mod.options.thresholds.checks, ['rate>0.99']);
      assert.equal(mod.options.thresholds.eod_contract_failures, undefined);

      // Even empty data envelope {} satisfies baseline assertApiSuccess
      resetHarness(makeRes({ success: true, data: {} }, 200));
      mod.default();
      assert.equal(checkFailCount, 0);
      assert.equal(counterTotals.get('eod_contract_failures') || 0, 0);
    }
  });

  it('preserves Authorization Bearer header in analysis-valuation.js across modes', async () => {
    for (const mode of [undefined, 'structural', 'fixture']) {
      const env = {
        BASE_URL: 'http://127.0.0.1:18080',
        PORTFOLIO_ID: '9001',
        ACCESS_TOKEN: 'my-secret-token',
        K6_MODE: 'quick',
      };
      if (mode) env.EOD_CONTRACT_MODE = mode;
      const mod = await importScenario('analysis-valuation.js', env);
      resetHarness(makeRes(validFixtureValuationPayload(), 200));
      mod.default();
      assert.equal(lastHttpGet.params.headers.Authorization, 'Bearer my-secret-token');
      assert.equal(checkFailCount, 0);
    }
  });

  it('fails fast during init on unknown EOD_CONTRACT_MODE typos such as fixtur', async () => {
    await assert.rejects(
      () =>
        importScenario('market-indexes.js', {
          BASE_URL: 'http://127.0.0.1:18080',
          EOD_CONTRACT_MODE: 'fixtur',
        }),
      /Invalid EOD_CONTRACT_MODE="fixtur"/,
    );
    assert.throws(() => checks.resolveContractMode('strict'), /Invalid EOD_CONTRACT_MODE/);
    assert.equal(checks.resolveContractMode(undefined), 'baseline');
    assert.equal(checks.resolveContractMode(''), 'baseline');
    assert.equal(checks.resolveContractMode(' FIXTURE '), 'fixture');
  });

  it('enforces strictReadOptions only when EOD_CONTRACT_MODE=fixture', async () => {
    const structMod = await importScenario('market-indexes.js', {
      BASE_URL: 'http://127.0.0.1:18080',
      K6_MODE: 'quick',
      EOD_CONTRACT_MODE: 'structural',
    });
    assert.equal(structMod.options.scenarios.market_indexes.executor, 'constant-vus');
    assert.deepEqual(structMod.options.thresholds.checks, ['rate>0.9']);
    assert.equal(structMod.options.thresholds.eod_contract_failures, undefined);

    const fixMod = await importScenario('market-indexes.js', {
      BASE_URL: 'http://127.0.0.1:18080',
      K6_MODE: 'quick',
      EOD_CONTRACT_MODE: 'fixture',
    });
    assert.equal(fixMod.options.scenarios.market_indexes.executor, 'constant-vus');
    assert.deepEqual(fixMod.options.thresholds.http_req_failed, ['rate==0']);
    assert.deepEqual(fixMod.options.thresholds.checks, ['rate==1']);
    assert.deepEqual(fixMod.options.thresholds.eod_contract_failures, ['count==0']);
  });
});

describe('ISO calendar date & numeric scale validation', () => {
  it('accepts valid leap dates and rejects impossible calendar dates', () => {
    assert.equal(checks.isIsoDate('2024-02-29'), true);
    assert.equal(checks.isIsoDate('2026-11-18'), true);
    assert.equal(checks.isIsoDate('2026-02-30'), false);
    assert.equal(checks.isIsoDate('2025-02-29'), false);
    assert.equal(checks.isIsoDate('2026-13-01'), false);
    assert.equal(checks.isIsoDate('2026-00-10'), false);
    assert.equal(checks.isIsoDate('2026-04-31'), false);
    assert.equal(checks.isIsoDate('2026-11-18T00:00:00Z'), false);
  });

  it('validates DTO decimal scales accurately', () => {
    assert.equal(checks.hasMaxScale(2050000, 0), true);
    assert.equal(checks.hasMaxScale(2050000.00005, 0), false);
    assert.equal(checks.hasMaxScale(5.0, 2), true);
    assert.equal(checks.hasMaxScale(5.01, 2), true);
    assert.equal(checks.hasMaxScale(5.009, 2), false);
    assert.equal(checks.hasMaxScale(5.1282, 4), true);
    assert.equal(checks.hasMaxScale(5.12824, 4), false);
    assert.equal(checks.hasMaxScale(Number.NaN, 4), false);
  });
});

describe('Structural mode live/staging DTO compatibility & invariant checks', () => {
  it('accepts empty supply ranking, null/non-null effectiveDate, and 2026-11-19 trading date', () => {
    resetHarness();
    assert.equal(
      checks.assertStockSupplyRankingContract(
        makeRes({
          success: true,
          data: {
            requestedDate: null,
            effectiveDate: null,
            institutionItems: [],
            foreignItems: [],
          },
        }),
        null,
        { direction: 'BUY', limit: 10, mode: 'structural' },
      ),
      true,
    );

    resetHarness();
    assert.equal(
      checks.assertStockSupplyRankingContract(
        makeRes({
          success: true,
          data: {
            requestedDate: null,
            effectiveDate: '2026-11-19',
            institutionItems: [],
            foreignItems: [],
          },
        }),
        null,
        { direction: 'BUY', limit: 10, mode: 'structural' },
      ),
      true,
    );
  });

  it('rejects supply ranking sign or ordering bugs in structural mode', () => {
    resetHarness();
    assert.equal(
      checks.assertStockSupplyRankingContract(
        makeRes({
          success: true,
          data: {
            effectiveDate: '2026-11-19',
            institutionItems: [
              {
                ticker: '005930',
                stockName: 'Samsung',
                sectorName: 'Tech',
                currentPrice: 70000,
                fluctuationRate: 1.5,
                netBuyingQuantity: -100,
                netBuyingAmount: -7000000,
                transactionAmount: 100000000,
              },
            ],
            foreignItems: [],
          },
        }),
        null,
        { direction: 'BUY', limit: 10, mode: 'structural' },
      ),
      false,
    );
    assert.equal(counterTotals.get('eod_contract_failures'), 1);
  });

  it('accepts valid structural market indexes with null weather/fluctuationRate and 2026-11-19', () => {
    resetHarness();
    assert.equal(
      checks.assertMarketIndexesContract(
        makeRes({
          success: true,
          data: {
            indexes: [
              {
                ticker: '0001',
                name: 'KOSPI',
                currentPrice: 2625.1234,
                fluctuationRate: null,
                fluctuationAmount: null,
                history: [{ date: '2026-11-19', close: 2625.1234 }],
              },
            ],
            weather: null,
          },
        }),
        null,
        { mode: 'structural' },
      ),
      true,
    );
  });

  it('accepts empty benchmarks when includeBenchmark=true and weekly null volume in structural mode', () => {
    resetHarness();
    assert.equal(
      checks.assertStockPriceHistoryContract(
        makeRes({
          success: true,
          data: {
            ticker: '005930',
            currency: 'KRW',
            stockName: 'Samsung',
            benchmarkName: 'KOSPI',
            prices: [
              {
                date: '2026-11-19',
                open: 70000,
                high: 71000,
                low: 69500,
                close: 70500,
                adjClose: 70500,
                volume: null,
                transactionAmt: null,
                ma5: null,
                ma20: null,
                ma60: null,
                ma120: null,
              },
            ],
            benchmarks: [],
          },
        }),
        null,
        {
          ticker: '005930',
          frequency: 'WEEKLY',
          includeBenchmark: 'true',
          mode: 'structural',
        },
      ),
      true,
    );
  });

  it('rejects OHLC inversion and impossible calendar dates in structural price history', () => {
    resetHarness();
    assert.equal(
      checks.assertStockPriceHistoryContract(
        makeRes({
          success: true,
          data: {
            ticker: '005930',
            currency: 'KRW',
            prices: [
              {
                date: '2026-02-30',
                open: 100,
                high: 110,
                low: 90,
                close: 105,
                adjClose: 105,
                volume: 10,
                transactionAmt: 1000,
              },
            ],
            benchmarks: [],
          },
        }),
        null,
        { ticker: '005930', includeBenchmark: 'false', mode: 'structural' },
      ),
      false,
    );

    resetHarness();
    assert.equal(
      checks.assertStockPriceHistoryContract(
        makeRes({
          success: true,
          data: {
            ticker: '005930',
            currency: 'KRW',
            prices: [
              {
                date: '2026-11-18',
                open: 100,
                high: 102,
                low: 99,
                close: 105, // high < close inversion!
                adjClose: 105,
                volume: 10,
                transactionAmt: 1000,
              },
            ],
            benchmarks: [],
          },
        }),
        null,
        { ticker: '005930', includeBenchmark: 'false', mode: 'structural' },
      ),
      false,
    );
  });

  it('accepts PARTIAL, UNAVAILABLE, and zero-value COMPLETE portfolio valuations in structural mode', () => {
    const partialPayload = {
      success: true,
      data: {
        totalPurchaseAmount: 100000,
        currentTotalValue: null,
        totalProfitLoss: null,
        totalReturnRate: null,
        dailyProfitLoss: null,
        dailyReturnRate: null,
        valuationStatus: 'PARTIAL',
        asOfDate: '2026-11-19',
        missingSymbols: ['000660'],
      },
    };
    resetHarness();
    assert.equal(
      checks.assertAnalysisValuationContract(makeRes(partialPayload), null, {
        portfolioId: '18',
        mode: 'structural',
      }),
      true,
    );

    const zeroCompletePayload = {
      success: true,
      data: {
        totalPurchaseAmount: 0,
        currentTotalValue: 0,
        totalProfitLoss: 0,
        totalReturnRate: 0,
        dailyProfitLoss: 0,
        dailyReturnRate: null,
        valuationStatus: 'COMPLETE',
        asOfDate: '2026-11-18',
        missingSymbols: [],
      },
    };
    resetHarness();
    assert.equal(
      checks.assertAnalysisValuationContract(makeRes(zeroCompletePayload), null, {
        portfolioId: '18',
        mode: 'structural',
      }),
      true,
    );
  });

  it('rejects COMPLETE valuation accounting mismatch in structural mode', () => {
    const broken = validFixtureValuationPayload();
    broken.data.currentTotalValue += 1;
    resetHarness();
    assert.equal(
      checks.assertAnalysisValuationContract(makeRes(broken), null, {
        portfolioId: '9001',
        mode: 'structural',
      }),
      false,
    );
    assert.equal(counterTotals.get('eod_contract_failures'), 1);
  });
});

describe('Fixture mode strictness & closure of previous blind spots', () => {
  it('passes all four synthetic fixture payloads even when stubbed zero fields vary', () => {
    resetHarness();
    assert.equal(
      checks.assertMarketIndexesContract(makeRes(validFixtureMarketIndexesPayload()), EXPECTED, {
        mode: 'fixture',
      }),
      true,
    );

    resetHarness();
    assert.equal(
      checks.assertStockSupplyRankingContract(makeRes(validFixtureSupplyPayload('BUY')), EXPECTED, {
        direction: 'BUY',
        limit: 10,
        mode: 'fixture',
      }),
      true,
    );

    resetHarness();
    assert.equal(
      checks.assertStockSupplyRankingContract(makeRes(validFixtureSupplyPayload('SELL')), EXPECTED, {
        direction: 'SELL',
        limit: 10,
        mode: 'fixture',
      }),
      true,
    );

    resetHarness();
    assert.equal(
      checks.assertStockPriceHistoryContract(
        makeRes(validFixturePriceHistoryPayload(true)),
        EXPECTED,
        {
          ticker: '990101',
          frequency: 'DAILY',
          includeBenchmark: 'true',
          mode: 'fixture',
        },
      ),
      true,
    );

    resetHarness();
    assert.equal(
      checks.assertAnalysisValuationContract(makeRes(validFixtureValuationPayload()), EXPECTED, {
        portfolioId: '9001',
        mode: 'fixture',
      }),
      true,
    );
  });

  it('rejects 8th leaked index, missing exactCount, out-of-scale fluctuationRate, and corrupted intermediate history', () => {
    // 8th index leak
    const leaked = validFixtureMarketIndexesPayload();
    leaked.data.indexes.push({
      ticker: 'EXTRA',
      name: 'Leak',
      currentPrice: 100,
      fluctuationRate: 0,
      history: [{ date: '2026-11-18', close: 100 }],
    });
    resetHarness();
    assert.equal(
      checks.assertMarketIndexesContract(makeRes(leaked), EXPECTED, { mode: 'fixture' }),
      false,
    );

    // Missing exactCount in fixture config
    const noExactCount = structuredClone(EXPECTED);
    delete noExactCount.marketIndexes.exactCount;
    resetHarness();
    assert.equal(
      checks.assertMarketIndexesContract(
        makeRes(validFixtureMarketIndexesPayload()),
        noExactCount,
        { mode: 'fixture' },
      ),
      false,
    );

    // fluctuationRate=5.009 (out of scale=2)
    const badScale = validFixtureMarketIndexesPayload();
    badScale.data.indexes[0].fluctuationRate = 5.009;
    resetHarness();
    assert.equal(
      checks.assertMarketIndexesContract(makeRes(badScale), EXPECTED, { mode: 'fixture' }),
      false,
    );

    // Intermediate history[0].close mutated from 2480 to 99999
    const badHist = validFixtureMarketIndexesPayload();
    badHist.data.indexes[0].history[0].close = 99999;
    resetHarness();
    assert.equal(
      checks.assertMarketIndexesContract(makeRes(badHist), EXPECTED, { mode: 'fixture' }),
      false,
    );
  });

  it('rejects valuation and supply scale drift and excludedDates leaks in fixture mode', () => {
    const badScaleVal = validFixtureValuationPayload();
    badScaleVal.data.currentTotalValue = 2050000.00005;
    resetHarness();
    assert.equal(
      checks.assertAnalysisValuationContract(makeRes(badScaleVal), EXPECTED, {
        portfolioId: '9001',
        mode: 'fixture',
      }),
      false,
    );

    const badRateVal = validFixtureValuationPayload();
    badRateVal.data.totalReturnRate = 5.12824;
    resetHarness();
    assert.equal(
      checks.assertAnalysisValuationContract(makeRes(badRateVal), EXPECTED, {
        portfolioId: '9001',
        mode: 'fixture',
      }),
      false,
    );

    const badSupplyAmt = validFixtureSupplyPayload('BUY');
    badSupplyAmt.data.institutionItems[0].netBuyingAmount = 550000000.005;
    resetHarness();
    assert.equal(
      checks.assertStockSupplyRankingContract(makeRes(badSupplyAmt), EXPECTED, {
        direction: 'BUY',
        limit: 10,
        mode: 'fixture',
      }),
      false,
    );

    const leakedFutureDate = validFixturePriceHistoryPayload(false);
    leakedFutureDate.data.prices[2].date = '2099-12-31';
    resetHarness();
    assert.equal(
      checks.assertStockPriceHistoryContract(makeRes(leakedFutureDate), EXPECTED, {
        ticker: '990101',
        frequency: 'DAILY',
        includeBenchmark: 'false',
        mode: 'fixture',
      }),
      false,
    );
  });
});

describe('Load mode VU body cache preserves 1-failure sensitivity without redundant JSON.parse', () => {
  it('skips repeated JSON.parse on identical 200 responses and fails immediately on 1 corrupted response', async () => {
    const mod = await importScenario('market-indexes.js', {
      BASE_URL: 'http://127.0.0.1:18080',
      K6_MODE: 'standard',
      EOD_CONTRACT_MODE: 'fixture',
    });

    const validBody = JSON.stringify(validFixtureMarketIndexesPayload());
    const corruptedPayload = validFixtureMarketIndexesPayload();
    corruptedPayload.data.indexes[0].history[0].close = 12345;
    const corruptedBody = JSON.stringify(corruptedPayload);

    let parseCalls = 0;
    const origParse = JSON.parse;
    JSON.parse = (...args) => {
      parseCalls += 1;
      return origParse(...args);
    };

    try {
      resetHarness({
        status: 200,
        body: validBody,
        json() {
          return JSON.parse(this.body);
        },
      });

      for (let i = 0; i < 20; i += 1) {
        mod.default();
      }
      assert.equal(parseCalls, 1, 'identical 200 bodies must parse only once per VU');
      assert.equal(checkFailCount, 0);
      assert.equal(counterTotals.get('eod_contract_failures') || 0, 0);

      // 21st request returns a corrupted intermediate history close
      mockHttpResponse = {
        status: 200,
        body: corruptedBody,
        json() {
          return JSON.parse(this.body);
        },
      };
      mod.default();
      assert.equal(parseCalls, 2);
      assert.equal(checkFailCount, 1);
      assert.equal(counterTotals.get('eod_contract_failures'), 1);
    } finally {
      JSON.parse = origParse;
    }
  });
});
