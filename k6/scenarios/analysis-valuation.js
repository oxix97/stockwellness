import http from 'k6/http';
import { sleep } from 'k6';
import { requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';
import {
  assertApiSuccess,
  createAnalysisValuationValidator,
  resolveContractMode,
} from '../helpers/checks.js';
import { quickOptions, readOptions, strictReadOptions } from '../helpers/options.js';

const BASE_URL = requireEnv('BASE_URL');
const PORTFOLIO_ID = requireEnv('PORTFOLIO_ID');
const CONTRACT_MODE = resolveContractMode(__ENV.EOD_CONTRACT_MODE);
const IS_QUICK = (__ENV.K6_MODE || 'standard') === 'quick';
const EXPECTED = CONTRACT_MODE === 'fixture' ? JSON.parse(open('../fixtures/eod-read-expected.json')) : null;
const validateContract = CONTRACT_MODE === 'baseline'
  ? null
  : createAnalysisValuationValidator(EXPECTED, {
      portfolioId: PORTFOLIO_ID,
      mode: CONTRACT_MODE,
      enforceBodyCache: !IS_QUICK,
    });

export const options = CONTRACT_MODE === 'fixture'
  ? strictReadOptions('analysis_valuation')
  : (IS_QUICK ? quickOptions : readOptions)('analysis_valuation');

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/portfolios/${PORTFOLIO_ID}/analysis/valuation`, {
    headers: authHeaders(),
    tags: { api: 'analysis_valuation', type: 'read' },
  });

  if (CONTRACT_MODE === 'baseline') {
    assertApiSuccess('analysis valuation', res);
  } else {
    validateContract(res);
  }
  sleep(1);
}
