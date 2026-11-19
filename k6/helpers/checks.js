import { check } from 'k6';
import { Counter } from 'k6/metrics';

export const eodContractFailures = new Counter('eod_contract_failures');
export const VALID_CONTRACT_MODES = Object.freeze(['baseline', 'structural', 'fixture']);

let loggedFailureCount = 0;
const MAX_LOGGED_FAILURES = 10;
const DAYS_IN_MONTH = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
const VALID_VALUATION_STATUSES = new Set(['COMPLETE', 'PARTIAL', 'UNAVAILABLE']);

export function resolveContractMode(rawMode) {
  if (rawMode === undefined || rawMode === null || String(rawMode).trim() === '') {
    return 'baseline';
  }
  const normalized = String(rawMode).trim().toLowerCase();
  if (!VALID_CONTRACT_MODES.includes(normalized)) {
    throw new Error(
      `Invalid EOD_CONTRACT_MODE="${rawMode}". Expected one of: ${VALID_CONTRACT_MODES.join(', ')}`,
    );
  }
  return normalized;
}

export function assertApiSuccess(name, res) {
  return check(res, {
    [`${name} status is 200`]: (r) => r.status === 200,
    [`${name} has data`]: (r) => {
      const body = safeJson(r);
      return Boolean(body && Object.prototype.hasOwnProperty.call(body, 'data'));
    },
  });
}

function safeJson(res) {
  if (!res) return null;
  try {
    if (typeof res.body === 'string') {
      return JSON.parse(res.body);
    }
    return res.json();
  } catch (_e) {
    return null;
  }
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isFiniteNum(value) {
  return typeof value === 'number' && Number.isFinite(value);
}

function decimalScale(value) {
  if (!isFiniteNum(value)) return Number.POSITIVE_INFINITY;
  if (Number.isInteger(value)) return 0;
  const str = String(value).toLowerCase();
  const expIdx = str.indexOf('e');
  let mantissa = str;
  let exp = 0;
  if (expIdx !== -1) {
    mantissa = str.slice(0, expIdx);
    exp = Number.parseInt(str.slice(expIdx + 1), 10);
  }
  const dotIdx = mantissa.indexOf('.');
  const fracLen = dotIdx === -1 ? 0 : mantissa.length - dotIdx - 1;
  return Math.max(0, fracLen - exp);
}

export function hasMaxScale(value, maxScale) {
  if (!isFiniteNum(value)) return false;
  if (maxScale === 0) return Number.isInteger(value);
  return decimalScale(value) <= maxScale;
}

function approxEqual(actual, expected, tolerance = 1e-4) {
  return isFiniteNum(actual) && isFiniteNum(expected) && Math.abs(actual - expected) <= tolerance;
}

function exactOrScaledEqual(actual, expected, maxScale, tolerance = 1e-9) {
  if (!isFiniteNum(expected) || !hasMaxScale(actual, maxScale)) return false;
  if (maxScale === 0) return actual === expected;
  return Math.abs(actual - expected) <= tolerance;
}

function isLeapYear(year) {
  return year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
}

export function isIsoDate(value) {
  if (typeof value !== 'string' || value.length !== 10) return false;
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return false;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (month < 1 || month > 12) return false;
  const maxDay = month === 2 && isLeapYear(year) ? 29 : DAYS_IN_MONTH[month - 1];
  return day >= 1 && day <= maxDay;
}

function buildExcludedDateSet(expected) {
  if (!expected || !Array.isArray(expected.excludedDates)) return null;
  return new Set(expected.excludedDates);
}

function runContractValidation(name, res, state, validateData) {
  if (
    state.useBodyCache &&
    res &&
    res.status === 200 &&
    state.verifiedBody !== null &&
    typeof res.body === 'string' &&
    res.body === state.verifiedBody
  ) {
    return check(res, {
      [`${name} status is 200`]: () => true,
      [`${name} matches EOD contract`]: () => true,
    });
  }

  const statusOk = Boolean(res && res.status === 200);
  const body = statusOk ? safeJson(res) : null;
  const envelopeOk = Boolean(
    isObject(body) &&
      body.success === true &&
      Object.prototype.hasOwnProperty.call(body, 'data') &&
      isObject(body.data),
  );

  let failureMessage = null;
  if (!statusOk) {
    failureMessage = `expected HTTP 200, got ${res ? res.status : 'none'}`;
  } else if (!envelopeOk) {
    failureMessage = 'response envelope missing success=true or non-null object data';
  } else {
    failureMessage = validateData(body.data);
  }

  const contractOk = statusOk && envelopeOk && failureMessage === null;
  check(res, {
    [`${name} status is 200`]: () => statusOk,
    [`${name} matches EOD contract`]: () => contractOk,
  });

  if (contractOk) {
    if (state.useBodyCache && typeof res.body === 'string') {
      state.verifiedBody = res.body;
    }
    return true;
  }

  eodContractFailures.add(1);
  if (loggedFailureCount < MAX_LOGGED_FAILURES) {
    loggedFailureCount += 1;
    console.error(`[EOD contract failure][${name}] ${failureMessage}`);
  }
  return false;
}

export function createMarketIndexesValidator(expected, params = {}) {
  const mode = resolveContractMode(params.mode || 'structural');
  const excludedSet = mode === 'fixture' ? buildExcludedDateSet(expected) : null;
  const exp = expected && expected.marketIndexes ? expected.marketIndexes : null;
  const state = {
    useBodyCache: Boolean(params.enforceBodyCache),
    verifiedBody: null,
  };

  let expByTicker = null;
  if (mode === 'fixture' && exp && Array.isArray(exp.indexes)) {
    expByTicker = Object.create(null);
    for (const item of exp.indexes) {
      expByTicker[item.ticker] = item;
    }
  }

  const validateData = (data) => {
    const indexes = data.indexes;
    if (!Array.isArray(indexes) || indexes.length === 0) {
      return 'indexes must be a non-empty array';
    }

    const actualByTicker = mode === 'fixture' ? Object.create(null) : null;
    const seenTickers = new Set();

    for (let k = 0; k < indexes.length; k += 1) {
      const idx = indexes[k];
      if (!isObject(idx) || typeof idx.ticker !== 'string' || idx.ticker.length === 0) {
        return `indexes[${k}] missing ticker`;
      }
      if (seenTickers.has(idx.ticker)) {
        return `duplicate index ticker: ${idx.ticker}`;
      }
      seenTickers.add(idx.ticker);
      if (actualByTicker) {
        actualByTicker[idx.ticker] = idx;
      }

      if (!isFiniteNum(idx.currentPrice) || idx.currentPrice <= 0 || !hasMaxScale(idx.currentPrice, 4)) {
        return `index ${idx.ticker} currentPrice invalid: ${idx.currentPrice}`;
      }
      if (idx.fluctuationRate !== null && !hasMaxScale(idx.fluctuationRate, 2)) {
        return `index ${idx.ticker} fluctuationRate invalid/out-of-scale: ${idx.fluctuationRate}`;
      }
      if (!Array.isArray(idx.history) || idx.history.length === 0) {
        return `index ${idx.ticker} history is empty`;
      }

      let prevDate = '';
      for (let i = 0; i < idx.history.length; i += 1) {
        const pt = idx.history[i];
        if (!isObject(pt) || !isIsoDate(pt.date) || !isFiniteNum(pt.close) || pt.close <= 0 || !hasMaxScale(pt.close, 4)) {
          return `index ${idx.ticker} history[${i}] invalid`;
        }
        if (excludedSet && excludedSet.has(pt.date)) {
          return `index ${idx.ticker} leaked excluded date ${pt.date}`;
        }
        if (i > 0 && prevDate >= pt.date) {
          return `index ${idx.ticker} history not strictly ascending at ${pt.date}`;
        }
        prevDate = pt.date;
      }

      const lastPt = idx.history[idx.history.length - 1];
      if (!approxEqual(lastPt.close, idx.currentPrice, 1e-4)) {
        return `index ${idx.ticker} currentPrice=${idx.currentPrice} != latest history close=${lastPt.close}`;
      }
    }

    const weather = data.weather;
    if (weather !== null && weather !== undefined) {
      if (!isObject(weather) || !isIsoDate(weather.asOfDate)) {
        return `weather invalid asOfDate: ${weather ? weather.asOfDate : null}`;
      }
      if (excludedSet && excludedSet.has(weather.asOfDate)) {
        return `weather leaked excluded asOfDate: ${weather.asOfDate}`;
      }
    } else if (mode === 'fixture' && exp && exp.expectedWeather) {
      return 'weather must not be null in fixture mode';
    }

    if (mode === 'fixture') {
      if (!exp || !expByTicker) {
        return 'missing expected marketIndexes fixture';
      }
      if (!Number.isInteger(exp.exactCount) || exp.exactCount <= 0) {
        return `invalid fixture exactCount: ${exp.exactCount}`;
      }
      if (indexes.length !== exp.exactCount) {
        return `expected exactCount=${exp.exactCount} indexes, got ${indexes.length}`;
      }

      for (let k = 0; k < exp.indexes.length; k += 1) {
        const expIdx = exp.indexes[k];
        const actual = actualByTicker[expIdx.ticker];
        if (!actual) {
          return `missing expected benchmark ${expIdx.ticker}`;
        }
        if (actual.name !== expIdx.name) {
          return `${expIdx.ticker} name=${actual.name}, expected=${expIdx.name}`;
        }
        if (!exactOrScaledEqual(actual.currentPrice, expIdx.currentPrice, 4)) {
          return `${expIdx.ticker} currentPrice=${actual.currentPrice}, expected=${expIdx.currentPrice}`;
        }
        if (!exactOrScaledEqual(actual.fluctuationRate, expIdx.fluctuationRate, 2)) {
          return `${expIdx.ticker} fluctuationRate=${actual.fluctuationRate}, expected=${expIdx.fluctuationRate}`;
        }
        if (!Array.isArray(expIdx.history) || expIdx.history.length === 0) {
          return `${expIdx.ticker} fixture missing expected history array`;
        }
        if (actual.history.length !== expIdx.history.length) {
          return `${expIdx.ticker} history length=${actual.history.length}, expected=${expIdx.history.length}`;
        }
        for (let i = 0; i < expIdx.history.length; i += 1) {
          const expPt = expIdx.history[i];
          const actPt = actual.history[i];
          if (actPt.date !== expPt.date) {
            return `${expIdx.ticker} history[${i}].date=${actPt.date}, expected=${expPt.date}`;
          }
          if (!exactOrScaledEqual(actPt.close, expPt.close, 4)) {
            return `${expIdx.ticker} history[${i}].close=${actPt.close}, expected=${expPt.close}`;
          }
        }
      }

      if (exp.expectedWeather) {
        if (!isObject(weather)) {
          return 'weather missing in fixture mode';
        }
        if (weather.weatherLevel !== exp.expectedWeather.weatherLevel) {
          return `weatherLevel=${weather.weatherLevel}, expected=${exp.expectedWeather.weatherLevel}`;
        }
        if (weather.reasonCode !== exp.expectedWeather.reasonCode) {
          return `reasonCode=${weather.reasonCode}, expected=${exp.expectedWeather.reasonCode}`;
        }
        if (weather.asOfDate !== exp.expectedWeather.asOfDate) {
          return `weather.asOfDate=${weather.asOfDate}, expected=${exp.expectedWeather.asOfDate}`;
        }
      }
    }

    return null;
  };

  return (res) => runContractValidation('market indexes', res, state, validateData);
}

export function assertMarketIndexesContract(res, expected, params = {}) {
  return createMarketIndexesValidator(expected, params)(res);
}

export function createStockSupplyRankingValidator(expected, params = {}) {
  const mode = resolveContractMode(params.mode || 'structural');
  const direction = String(params.direction || 'BUY').toUpperCase();
  const limit = Number(params.limit || 10);
  const excludedSet = mode === 'fixture' ? buildExcludedDateSet(expected) : null;
  const expRoot = expected && expected.supplyRanking ? expected.supplyRanking : null;
  const expDir = expRoot && expRoot[direction] ? expRoot[direction] : null;
  const expectedInst = expDir && Array.isArray(expDir.institutionItems) ? expDir.institutionItems.slice(0, limit) : null;
  const expectedFor = expDir && Array.isArray(expDir.foreignItems) ? expDir.foreignItems.slice(0, limit) : null;
  const state = {
    useBodyCache: Boolean(params.enforceBodyCache),
    verifiedBody: null,
  };

  const validateListStructural = (listName, items, effectiveIsNull) => {
    if (!Array.isArray(items)) {
      return `${listName} must be an array`;
    }
    if (effectiveIsNull) {
      if (items.length !== 0) {
        return `${listName} must be empty when effectiveDate is null`;
      }
      return null;
    }
    if (items.length > limit) {
      return `${listName} length ${items.length} exceeds limit ${limit}`;
    }

    for (let i = 0; i < items.length; i += 1) {
      const item = items[i];
      if (!isObject(item) || typeof item.ticker !== 'string' || item.ticker.length === 0) {
        return `${listName}[${i}] missing ticker`;
      }
      if (!Number.isInteger(item.netBuyingQuantity) || !isFiniteNum(item.netBuyingAmount) || !hasMaxScale(item.netBuyingAmount, 2)) {
        return `${listName}[${i}] (${item.ticker}) invalid netBuyingQuantity or netBuyingAmount`;
      }
      if (direction === 'BUY' && item.netBuyingAmount <= 0) {
        return `${listName}[${i}] (${item.ticker}) BUY netBuyingAmount must be > 0, got ${item.netBuyingAmount}`;
      }
      if (direction === 'SELL' && item.netBuyingAmount >= 0) {
        return `${listName}[${i}] (${item.ticker}) SELL netBuyingAmount must be < 0, got ${item.netBuyingAmount}`;
      }
      if (item.currentPrice === null) {
        if (item.fluctuationRate !== null || item.transactionAmount !== null) {
          return `${listName}[${i}] (${item.ticker}) null currentPrice must have null fluctuationRate/transactionAmount`;
        }
      } else {
        if (!isFiniteNum(item.currentPrice) || item.currentPrice <= 0 || !hasMaxScale(item.currentPrice, 4)) {
          return `${listName}[${i}] (${item.ticker}) invalid currentPrice=${item.currentPrice}`;
        }
        if (item.fluctuationRate !== null && !isFiniteNum(item.fluctuationRate)) {
          return `${listName}[${i}] (${item.ticker}) invalid fluctuationRate=${item.fluctuationRate}`;
        }
        if (!isFiniteNum(item.transactionAmount) || item.transactionAmount < 0 || !hasMaxScale(item.transactionAmount, 2)) {
          return `${listName}[${i}] (${item.ticker}) invalid transactionAmount=${item.transactionAmount}`;
        }
      }

      if (i > 0) {
        const prev = items[i - 1];
        if (direction === 'BUY') {
          if (prev.netBuyingAmount < item.netBuyingAmount) {
            return `${listName} out of BUY DESC order at ${prev.ticker} -> ${item.ticker}`;
          }
          if (prev.netBuyingAmount === item.netBuyingAmount && prev.ticker >= item.ticker) {
            return `${listName} tie-break violation at ${prev.ticker} -> ${item.ticker}`;
          }
        } else if (direction === 'SELL') {
          if (prev.netBuyingAmount > item.netBuyingAmount) {
            return `${listName} out of SELL ASC order at ${prev.ticker} -> ${item.ticker}`;
          }
          if (prev.netBuyingAmount === item.netBuyingAmount && prev.ticker >= item.ticker) {
            return `${listName} tie-break violation at ${prev.ticker} -> ${item.ticker}`;
          }
        }
      }
    }
    return null;
  };

  const validateListFixture = (listName, actualList, expectedList) => {
    if (actualList.length !== expectedList.length) {
      return `${listName} length=${actualList.length}, expected=${expectedList.length}`;
    }
    for (let i = 0; i < expectedList.length; i += 1) {
      const expItem = expectedList[i];
      const actItem = actualList[i];
      if (actItem.ticker !== expItem.ticker) {
        return `${listName}[${i}].ticker=${actItem.ticker}, expected=${expItem.ticker}`;
      }
      if (actItem.stockName !== expItem.stockName) {
        return `${listName}[${i}] (${actItem.ticker}).stockName=${actItem.stockName}, expected=${expItem.stockName}`;
      }
      if (actItem.sectorName !== expItem.sectorName) {
        return `${listName}[${i}] (${actItem.ticker}).sectorName=${actItem.sectorName}, expected=${expItem.sectorName}`;
      }
      if (expItem.currentPrice === null) {
        if (actItem.currentPrice !== null || actItem.fluctuationRate !== null || actItem.transactionAmount !== null) {
          return `${listName}[${i}] (${actItem.ticker}) expected null prices for missing FINAL price`;
        }
      } else {
        if (!exactOrScaledEqual(actItem.currentPrice, expItem.currentPrice, 4)) {
          return `${listName}[${i}] (${actItem.ticker}).currentPrice=${actItem.currentPrice}, expected=${expItem.currentPrice}`;
        }
        if (!exactOrScaledEqual(actItem.fluctuationRate, expItem.fluctuationRate, 4)) {
          return `${listName}[${i}] (${actItem.ticker}).fluctuationRate=${actItem.fluctuationRate}, expected=${expItem.fluctuationRate}`;
        }
        if (!exactOrScaledEqual(actItem.transactionAmount, expItem.transactionAmount, 2)) {
          return `${listName}[${i}] (${actItem.ticker}).transactionAmount=${actItem.transactionAmount}, expected=${expItem.transactionAmount}`;
        }
      }
      if (actItem.netBuyingQuantity !== expItem.netBuyingQuantity) {
        return `${listName}[${i}] (${actItem.ticker}).netBuyingQuantity=${actItem.netBuyingQuantity}, expected=${expItem.netBuyingQuantity}`;
      }
      if (!exactOrScaledEqual(actItem.netBuyingAmount, expItem.netBuyingAmount, 2)) {
        return `${listName}[${i}] (${actItem.ticker}).netBuyingAmount=${actItem.netBuyingAmount}, expected=${expItem.netBuyingAmount}`;
      }
    }
    return null;
  };

  const validateData = (data) => {
    const effectiveIsNull = data.effectiveDate === null;
    if (!effectiveIsNull) {
      if (!isIsoDate(data.effectiveDate)) {
        return `invalid effectiveDate: ${data.effectiveDate}`;
      }
      if (excludedSet && excludedSet.has(data.effectiveDate)) {
        return `effectiveDate leaked excluded date: ${data.effectiveDate}`;
      }
    }

    const instErr = validateListStructural('institutionItems', data.institutionItems, effectiveIsNull);
    if (instErr) return instErr;
    const forErr = validateListStructural('foreignItems', data.foreignItems, effectiveIsNull);
    if (forErr) return forErr;

    if (mode === 'fixture') {
      if (!expRoot || !expectedInst || !expectedFor) {
        return `no expected fixture supplyRanking entry for direction=${direction}`;
      }
      if (data.effectiveDate !== expRoot.effectiveDate) {
        return `effectiveDate=${data.effectiveDate}, expected=${expRoot.effectiveDate}`;
      }
      const fixInstErr = validateListFixture('institutionItems', data.institutionItems, expectedInst);
      if (fixInstErr) return fixInstErr;
      const fixForErr = validateListFixture('foreignItems', data.foreignItems, expectedFor);
      if (fixForErr) return fixForErr;
    }

    return null;
  };

  return (res) => runContractValidation('stock supply ranking', res, state, validateData);
}

export function assertStockSupplyRankingContract(res, expected, params = {}) {
  return createStockSupplyRankingValidator(expected, params)(res);
}

const PRICE_FIELDS_4DP = ['open', 'high', 'low', 'close', 'adjClose', 'ma5', 'ma20', 'ma60', 'ma120'];

export function createStockPriceHistoryValidator(expected, params = {}) {
  const mode = resolveContractMode(params.mode || 'structural');
  const ticker = params.ticker;
  const frequency = String(params.frequency || 'DAILY').toUpperCase();
  const includeBenchmark = String(params.includeBenchmark || 'false').toLowerCase() === 'true';
  const excludedSet = mode === 'fixture' ? buildExcludedDateSet(expected) : null;
  const expStock =
    mode === 'fixture' && expected && expected.priceHistory && expected.priceHistory.stocks
      ? expected.priceHistory.stocks[ticker]
      : null;
  const expFreq = expStock ? expStock[frequency] : null;
  const state = {
    useBodyCache: Boolean(params.enforceBodyCache),
    verifiedBody: null,
  };

  const validateData = (data) => {
    if (ticker && data.ticker !== ticker) {
      return `ticker=${data.ticker}, expected=${ticker}`;
    }
    if (typeof data.currency !== 'string' || data.currency.length !== 3) {
      return `invalid currency: ${data.currency}`;
    }

    const prices = data.prices;
    if (!Array.isArray(prices) || prices.length === 0) {
      return 'prices must be a non-empty array';
    }

    let prevDate = '';
    for (let i = 0; i < prices.length; i += 1) {
      const p = prices[i];
      if (!isObject(p) || !isIsoDate(p.date)) {
        return `prices[${i}] invalid date: ${p ? p.date : null}`;
      }
      if (excludedSet && excludedSet.has(p.date)) {
        return `prices[${i}] leaked excluded date ${p.date}`;
      }
      if (i > 0 && prevDate >= p.date) {
        return `prices not strictly ascending at ${p.date}`;
      }
      prevDate = p.date;

      if (
        !hasMaxScale(p.open, 4) ||
        !hasMaxScale(p.high, 4) ||
        !hasMaxScale(p.low, 4) ||
        !hasMaxScale(p.close, 4)
      ) {
        return `prices[${i}] (${p.date}) OHLC non-numeric or out-of-scale`;
      }
      if (
        p.open <= 0 ||
        p.high <= 0 ||
        p.low <= 0 ||
        p.close <= 0 ||
        p.high < p.low ||
        p.high < p.open ||
        p.high < p.close ||
        p.low > p.open ||
        p.low > p.close
      ) {
        return `prices[${i}] (${p.date}) OHLC invariant violated`;
      }
      if (p.volume !== null && (!Number.isInteger(p.volume) || p.volume < 0)) {
        return `prices[${i}] (${p.date}) invalid volume=${p.volume}`;
      }
      if (p.adjClose !== null && (p.adjClose <= 0 || !hasMaxScale(p.adjClose, 4))) {
        return `prices[${i}] (${p.date}) invalid adjClose=${p.adjClose}`;
      }
      if (p.transactionAmt !== null && (!isFiniteNum(p.transactionAmt) || p.transactionAmt < 0 || !hasMaxScale(p.transactionAmt, 2))) {
        return `prices[${i}] (${p.date}) invalid transactionAmt=${p.transactionAmt}`;
      }
    }

    const benchmarks = data.benchmarks;
    if (!Array.isArray(benchmarks)) {
      return 'benchmarks must be an array';
    }
    if (!includeBenchmark) {
      if (benchmarks.length !== 0) {
        return 'includeBenchmark=false must return empty benchmarks array';
      }
    } else {
      let prevBenchDate = '';
      for (let i = 0; i < benchmarks.length; i += 1) {
        const b = benchmarks[i];
        if (!isObject(b) || !isIsoDate(b.date) || !hasMaxScale(b.returnRate, 4)) {
          return `invalid benchmark point at index ${i}`;
        }
        if (excludedSet && excludedSet.has(b.date)) {
          return `benchmarks[${i}] leaked excluded date ${b.date}`;
        }
        if (i > 0 && prevBenchDate >= b.date) {
          return `benchmarks not strictly ascending at ${b.date}`;
        }
        prevBenchDate = b.date;
      }
    }

    if (mode === 'fixture') {
      if (!expStock || !expFreq) {
        return `no expected priceHistory fixture for ticker=${ticker}, frequency=${frequency}`;
      }
      if (data.currency !== expStock.currency) {
        return `currency=${data.currency}, expected=${expStock.currency}`;
      }
      if (data.stockName !== expStock.stockName) {
        return `stockName=${data.stockName}, expected=${expStock.stockName}`;
      }
      if (data.benchmarkName !== expStock.benchmarkName) {
        return `benchmarkName=${data.benchmarkName}, expected=${expStock.benchmarkName}`;
      }
      if (prices.length !== expFreq.pointCount) {
        return `prices.length=${prices.length}, expected=${expFreq.pointCount}`;
      }
      for (let i = 0; i < expFreq.prices.length; i += 1) {
        const ep = expFreq.prices[i];
        const ap = prices[i];
        if (ap.date !== ep.date) {
          return `prices[${i}].date=${ap.date}, expected=${ep.date}`;
        }
        for (let f = 0; f < PRICE_FIELDS_4DP.length; f += 1) {
          const field = PRICE_FIELDS_4DP[f];
          if (ep[field] === null) {
            if (ap[field] !== null) {
              return `prices[${i}] (${ep.date}).${field}=${ap[field]}, expected=null`;
            }
          } else if (!exactOrScaledEqual(ap[field], ep[field], 4)) {
            return `prices[${i}] (${ep.date}).${field}=${ap[field]}, expected=${ep[field]}`;
          }
        }
        if (!Number.isInteger(ap.volume) || ap.volume !== ep.volume) {
          return `prices[${i}] (${ep.date}).volume=${ap.volume}, expected=${ep.volume}`;
        }
        if (!exactOrScaledEqual(ap.transactionAmt, ep.transactionAmt, 2)) {
          return `prices[${i}] (${ep.date}).transactionAmt=${ap.transactionAmt}, expected=${ep.transactionAmt}`;
        }
      }

      if (includeBenchmark && Array.isArray(expFreq.benchmarks)) {
        if (benchmarks.length !== expFreq.benchmarks.length) {
          return `benchmarks.length=${benchmarks.length}, expected=${expFreq.benchmarks.length}`;
        }
        for (let i = 0; i < expFreq.benchmarks.length; i += 1) {
          const eb = expFreq.benchmarks[i];
          const ab = benchmarks[i];
          if (ab.date !== eb.date || !exactOrScaledEqual(ab.returnRate, eb.returnRate, 4)) {
            return `benchmarks[${i}]={${ab.date},${ab.returnRate}}, expected={${eb.date},${eb.returnRate}}`;
          }
        }
      }
    }

    return null;
  };

  return (res) => runContractValidation('stock price history', res, state, validateData);
}

export function assertStockPriceHistoryContract(res, expected, params = {}) {
  return createStockPriceHistoryValidator(expected, params)(res);
}

export function createAnalysisValuationValidator(expected, params = {}) {
  const mode = resolveContractMode(params.mode || 'structural');
  const portfolioId = String(params.portfolioId || '');
  const excludedSet = mode === 'fixture' ? buildExcludedDateSet(expected) : null;
  const expPort =
    mode === 'fixture' && expected && expected.valuation && expected.valuation.portfolios
      ? expected.valuation.portfolios[portfolioId]
      : null;
  const state = {
    useBodyCache: Boolean(params.enforceBodyCache),
    verifiedBody: null,
  };

  const validateData = (data) => {
    if (!VALID_VALUATION_STATUSES.has(data.valuationStatus)) {
      return `invalid valuationStatus: ${data.valuationStatus}`;
    }

    if (data.asOfDate !== null) {
      if (!isIsoDate(data.asOfDate)) {
        return `invalid asOfDate: ${data.asOfDate}`;
      }
      if (excludedSet && excludedSet.has(data.asOfDate)) {
        return `asOfDate leaked excluded date: ${data.asOfDate}`;
      }
    }

    if (!Array.isArray(data.missingSymbols)) {
      return 'missingSymbols must be an array';
    }
    for (let i = 0; i < data.missingSymbols.length; i += 1) {
      if (typeof data.missingSymbols[i] !== 'string' || data.missingSymbols[i].length === 0) {
        return `missingSymbols[${i}] must be a non-empty string`;
      }
    }

    if (!hasMaxScale(data.totalPurchaseAmount, 0) || data.totalPurchaseAmount < 0) {
      return `totalPurchaseAmount invalid/out-of-scale: ${data.totalPurchaseAmount}`;
    }

    if (data.valuationStatus === 'COMPLETE') {
      if (data.missingSymbols.length !== 0) {
        return `COMPLETE valuation must have empty missingSymbols, got ${JSON.stringify(data.missingSymbols)}`;
      }
      if (!hasMaxScale(data.currentTotalValue, 0) || data.currentTotalValue < 0) {
        return `currentTotalValue invalid/out-of-scale: ${data.currentTotalValue}`;
      }
      if (!hasMaxScale(data.totalProfitLoss, 0)) {
        return `totalProfitLoss invalid/out-of-scale: ${data.totalProfitLoss}`;
      }
      if (!hasMaxScale(data.totalReturnRate, 4)) {
        return `totalReturnRate invalid/out-of-scale: ${data.totalReturnRate}`;
      }
      if (data.totalPurchaseAmount + data.totalProfitLoss !== data.currentTotalValue) {
        return `accounting mismatch: ${data.totalPurchaseAmount} + ${data.totalProfitLoss} != ${data.currentTotalValue}`;
      }
      if (data.totalPurchaseAmount === 0) {
        if (data.totalReturnRate !== 0) {
          return `totalReturnRate must be 0 when totalPurchaseAmount is 0, got ${data.totalReturnRate}`;
        }
      } else {
        const expectedRatePct = (data.totalProfitLoss / data.totalPurchaseAmount) * 100;
        if (!approxEqual(data.totalReturnRate, expectedRatePct, 0.0001)) {
          return `totalReturnRate mismatch: got ${data.totalReturnRate}, expected ~${expectedRatePct.toFixed(4)}`;
        }
      }

      if (data.dailyProfitLoss === null) {
        if (data.dailyReturnRate !== null) {
          return `dailyReturnRate must be null when dailyProfitLoss is null, got ${data.dailyReturnRate}`;
        }
      } else {
        if (!hasMaxScale(data.dailyProfitLoss, 0)) {
          return `dailyProfitLoss invalid/out-of-scale: ${data.dailyProfitLoss}`;
        }
        const previousTotalValue = data.currentTotalValue - data.dailyProfitLoss;
        if (previousTotalValue <= 0) {
          if (data.dailyReturnRate !== null) {
            return `dailyReturnRate must be null when previousTotalValue <= 0, got ${data.dailyReturnRate}`;
          }
        } else if (data.dailyReturnRate !== null) {
          if (!hasMaxScale(data.dailyReturnRate, 4)) {
            return `dailyReturnRate invalid/out-of-scale: ${data.dailyReturnRate}`;
          }
          const expectedDailyPct = (data.dailyProfitLoss / previousTotalValue) * 100;
          if (!approxEqual(data.dailyReturnRate, expectedDailyPct, 0.0001)) {
            return `dailyReturnRate mismatch: got ${data.dailyReturnRate}, expected ~${expectedDailyPct.toFixed(4)}`;
          }
        }
      }
    } else {
      if (data.missingSymbols.length === 0) {
        return `${data.valuationStatus} valuation must have non-empty missingSymbols`;
      }
      for (const nullField of ['currentTotalValue', 'totalProfitLoss', 'totalReturnRate', 'dailyProfitLoss', 'dailyReturnRate']) {
        if (data[nullField] !== null) {
          return `${data.valuationStatus} valuation must have ${nullField}=null, got ${data[nullField]}`;
        }
      }
    }

    if (mode === 'fixture') {
      if (!expPort) {
        return `no expected valuation fixture for portfolioId=${portfolioId}`;
      }
      if (data.asOfDate === null || data.asOfDate !== expPort.asOfDate) {
        return `asOfDate=${data.asOfDate}, expected=${expPort.asOfDate}`;
      }
      if (data.valuationStatus !== expPort.valuationStatus) {
        return `valuationStatus=${data.valuationStatus}, expected=${expPort.valuationStatus}`;
      }
      for (const intField of ['totalPurchaseAmount', 'currentTotalValue', 'totalProfitLoss', 'dailyProfitLoss']) {
        if (!exactOrScaledEqual(data[intField], expPort[intField], 0)) {
          return `${intField}=${data[intField]}, expected=${expPort[intField]}`;
        }
      }
      for (const rateField of ['totalReturnRate', 'dailyReturnRate']) {
        if (!exactOrScaledEqual(data[rateField], expPort[rateField], 4)) {
          return `${rateField}=${data[rateField]}, expected=${expPort[rateField]}`;
        }
      }
    }

    return null;
  };

  return (res) => runContractValidation('analysis valuation', res, state, validateData);
}

export function assertAnalysisValuationContract(res, expected, params = {}) {
  return createAnalysisValuationValidator(expected, params)(res);
}
