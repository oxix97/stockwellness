import http from 'k6/http';
import { requireEnv, optionalEnv } from '../helpers/env.js';

const BASE_URL = requireEnv('BASE_URL');
const LOGIN_EMAIL = optionalEnv('LOGIN_EMAIL', 'k6.live@example.com');
const LOGIN_NICKNAME = optionalEnv('LOGIN_NICKNAME', 'k6-live');
const LOGIN_TYPE = optionalEnv('LOGIN_TYPE', 'NONE');

export const options = {
  scenarios: {
    data_setup: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '10s',
    },
  },
};

export default function () {
  // 1. 로그인
  const loginPayload = JSON.stringify({ email: LOGIN_EMAIL, nickname: LOGIN_NICKNAME, loginType: LOGIN_TYPE });
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' }
  });
  
  if (loginRes.status !== 200 || !loginRes.json().data.accessToken) {
    console.error('Login failed');
    return;
  }
  const accessToken = loginRes.json().data.accessToken;
  const authHeader = { 'Authorization': `Bearer ${accessToken}`, 'Content-Type': 'application/json' };

  // 2. 포트폴리오 조회 및 생성
  let portfolioId = null;
  const pfListRes = http.get(`${BASE_URL}/api/v1/portfolios`, { headers: authHeader });
  if (pfListRes.status === 200 && pfListRes.json().data.length > 0) {
    portfolioId = pfListRes.json().data[0].id;
  } else {
    const pfPayload = JSON.stringify({
      name: 'k6-live-portfolio',
      description: 'k6 test portfolio',
      items: [{ symbol: '005930', quantity: 10, purchasePrice: 70000, currency: 'KRW', assetType: 'STOCK', targetWeight: 100 }]
    });
    const pfCreateRes = http.post(`${BASE_URL}/api/v1/portfolios`, pfPayload, { headers: authHeader });
    if (pfCreateRes.status === 200 || pfCreateRes.status === 201) {
      portfolioId = pfCreateRes.json().data;
    }
  }

  // 3. 관심 종목 그룹 조회 및 생성
  let groupId = null;
  const groupListRes = http.get(`${BASE_URL}/api/v1/watchlist/groups`, { headers: authHeader });
  if (groupListRes.status === 200 && groupListRes.json().data.length > 0) {
    groupId = groupListRes.json().data[0].id;
  } else {
    const groupPayload = JSON.stringify({ name: 'k6-live-group' });
    const groupCreateRes = http.post(`${BASE_URL}/api/v1/watchlist/groups`, groupPayload, { headers: authHeader });
    if (groupCreateRes.status === 200 || groupCreateRes.status === 201) {
      groupId = groupCreateRes.json().data;
    }
  }

  // 4. 유효한 섹터 코드 조회 (테스트용)
  let sectorCode = '0001'; // Default
  const sectorRankingRes = http.get(`${BASE_URL}/api/v1/sectors/ranking/fluctuation?limit=1`, { headers: authHeader });
  if (sectorRankingRes.status === 200 && sectorRankingRes.json().data.length > 0) {
    sectorCode = sectorRankingRes.json().data[0].sectorCode;
  }

  // 통합 결과 출력
  console.log(JSON.stringify({ accessToken, portfolioId, groupId, sectorCode }));
}
