// Independent oracle: BacktestEngineTest dca_one_year_exact_xirr_and_same_month_single_deposit.
const dates = ['2025-01-02', '2025-01-15', '2026-01-02'];
const benchmarkRates = [0, 5, 10];

export function backtestFixtureFailure(res, strategy) {
  if (!res || res.status !== 200) return 'HTTP status';
  let body;
  try { body = JSON.parse(res.body); } catch (_) { return 'invalid JSON'; }
  const data = body && body.data;
  if (!data || body.success === false || !['LUMP_SUM', 'DCA'].includes(strategy)) return 'missing data or strategy';
  const dca = strategy === 'DCA';
  if (data.primaryBenchmark !== 'KOSPI' || data.calculationMethod !== (dca ? 'DCA_XIRR_TWR' : 'LUMP_SUM_CAGR_TWR')) return 'benchmark or strategy';
  const equal = (value, expected) => typeof value === 'number' && Number.isFinite(value) && Math.abs(value - expected) <= 0.0001;
  if (!Array.isArray(data.dailyResults) || data.dailyResults.length !== 3) return 'daily results';
  for (let i = 0; i < dates.length; i++) {
    const day = data.dailyResults[i];
    const value = i === 0 ? 1000 : (i === 2 && dca ? 2100 : 1100);
    const invested = i === 2 && dca ? 2000 : 1000;
    if (!day || day.date !== dates[i] || !equal(day.totalValue, value) || !equal(day.totalInvested, invested)
        || !equal(day.totalValue - day.totalInvested, value - invested) || !equal(day.returnRate, i === 1 ? 10 : 0)
        || !equal(day.benchmarkReturnRate, benchmarkRates[i]) || !day.benchmarkReturnRates
        || !equal(day.benchmarkReturnRates.KOSPI, benchmarkRates[i])) return `daily result ${i}`;
  }
  if (!equal(data.totalReturnRate, 10) || !equal(data.timeWeightedReturnRate, 10)
      || (dca ? data.cagr !== null || !equal(data.xirr, 10) : !equal(data.cagr, 10) || data.xirr !== null)) return 'return metrics';
  return null;
}
