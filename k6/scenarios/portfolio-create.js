import { readOptions, quickOptions } from '../helpers/options.js';
import http from 'k6/http';
import { check } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { authHeaders } from '../helpers/headers.js';

const BASE_URL = requireEnv('BASE_URL');
const PORTFOLIO_NAME = optionalEnv('PORTFOLIO_NAME', 'k6-live-portfolio');
const PORTFOLIO_DESCRIPTION = optionalEnv('PORTFOLIO_DESCRIPTION', 'k6 live performance test portfolio');
const TICKER = optionalEnv('TICKER', '005930');

export const options = (__ENV.K6_MODE === 'quick' ? quickOptions : readOptions)('portfolio_create');

export default function () {
  const payload = JSON.stringify({
    name: PORTFOLIO_NAME,
    description: PORTFOLIO_DESCRIPTION,
    items: [
      {
        symbol: TICKER,
        quantity: 10,
        purchasePrice: 70000,
        currency: 'KRW',
        assetType: 'STOCK',
        targetWeight: 100,
      },
    ],
  });

  const res = http.post(`${BASE_URL}/api/v1/portfolios`, payload, {
    headers: authHeaders(),
    tags: { api: 'portfolio_create', type: 'write' },
  });

  check(res, {
    'portfolio create status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'portfolio create has id': (r) => {
      const body = r.json();
      return body && typeof body.data === 'number';
    },
  });

  const body = res.json();
  console.log(JSON.stringify({ portfolioId: body.data }));
}
