# k6 Test Data Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** k6 성능 테스트 실행 전 `refresh-token.sh`를 통해 토큰뿐만 아니라 `PORTFOLIO_ID`와 `GROUP_ID`를 자동으로 조회/생성하여 `.env`에 주입함으로써, 인증 필요한 API 테스트 시 발생하는 100% 실패 문제를 해결합니다.

**Architecture:** 기존 `auth-login.js` 대신 로그인, 포트폴리오 조회/생성, 관심 종목 그룹 조회/생성을 한 번에 수행하여 JSON 결과를 출력하는 새로운 k6 시나리오(`data-setup.js`)를 작성합니다. `refresh-token.sh`는 이 스크립트를 실행하여 반환된 통합 JSON 데이터를 `jq`로 파싱해 `.env` 파일을 갱신합니다.

**Tech Stack:** Bash, k6, JavaScript, jq

---

### Task 1: 데이터 셋업용 k6 시나리오(`data-setup.js`) 생성

**Files:**
- Create: `k6/scenarios/data-setup.js`

- [x] **Step 1: 통합 데이터 셋업 스크립트 작성**

새로운 파일 `k6/scenarios/data-setup.js`를 생성하고 다음 코드를 작성합니다:

```javascript
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

  // 통합 결과 출력
  console.log(JSON.stringify({ accessToken, portfolioId, groupId }));
}
```

- [ ] **Step 2: Commit**
```bash
git add k6/scenarios/data-setup.js
git commit -m "test(k6): add data-setup scenario for automatic env population"
```

---

### Task 2: `refresh-token.sh` 스크립트 갱신

**Files:**
- Modify: `k6/refresh-token.sh`

- [ ] **Step 1: 스크립트 수정하여 `data-setup.js` 연동 및 ID 추출/주입 적용**

`k6/refresh-token.sh` 파일 내용을 다음과 같이 수정합니다. (Sed 구문에서 `PORTFOLIO_ID`와 `GROUP_ID` 업데이트 구문을 추가합니다):

```bash
#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
COMPOSE_FILE="$SCRIPT_DIR/compose.k6.yaml"

echo "==> [Auth] 테스트 데이터 및 인증 토큰 갱신을 시작합니다..."

# 1. data-setup.js 시나리오 실행 및 JSON 응답 추출
# level=info msg= 내부의 JSON 문자열을 추출하고 이스케이프 해제
RAW_OUTPUT=$(docker compose -f "$COMPOSE_FILE" run --rm k6 run /scripts/scenarios/data-setup.js 2>&1)
TOKEN_JSON=$(echo "$RAW_OUTPUT" | grep 'level=info msg=' | sed 's/.*msg="\(.*\)".*/\1/' | sed 's/\\"/"/g' | head -n 1)

if [ -z "$TOKEN_JSON" ]; then
    echo "ERROR: 데이터 추출에 실패했습니다. 로그를 확인하세요."
    # 디버깅용: 출력 내용 확인
    echo "$RAW_OUTPUT" | grep 'level=info'
    exit 1
fi

# 2. jq를 사용하여 토큰 및 ID 필드 추출
NEW_TOKEN=$(echo "$TOKEN_JSON" | jq -r '.accessToken // empty')
NEW_PF_ID=$(echo "$TOKEN_JSON" | jq -r '.portfolioId // empty')
NEW_GROUP_ID=$(echo "$TOKEN_JSON" | jq -r '.groupId // empty')

if [ -z "$NEW_TOKEN" ]; then
    echo "ERROR: 유효한 accessToken을 찾을 수 없습니다."
    exit 1
fi

# 3. .env 파일 업데이트 (macOS 및 Linux 호환)
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s|^ACCESS_TOKEN=.*|ACCESS_TOKEN=$NEW_TOKEN|" "$ENV_FILE"
    [[ -n "$NEW_PF_ID" ]] && sed -i '' "s|^PORTFOLIO_ID=.*|PORTFOLIO_ID=$NEW_PF_ID|" "$ENV_FILE"
    [[ -n "$NEW_GROUP_ID" ]] && sed -i '' "s|^GROUP_ID=.*|GROUP_ID=$NEW_GROUP_ID|" "$ENV_FILE"
else
    sed -i "s|^ACCESS_TOKEN=.*|ACCESS_TOKEN=$NEW_TOKEN|" "$ENV_FILE"
    [[ -n "$NEW_PF_ID" ]] && sed -i "s|^PORTFOLIO_ID=.*|PORTFOLIO_ID=$NEW_PF_ID|" "$ENV_FILE"
    [[ -n "$NEW_GROUP_ID" ]] && sed -i "s|^GROUP_ID=.*|GROUP_ID=$NEW_GROUP_ID|" "$ENV_FILE"
fi

echo "SUCCESS: .env 파일이 성공적으로 갱신되었습니다."
echo "- Token (Prefix): ${NEW_TOKEN:0:20}..."
echo "- Portfolio ID: $NEW_PF_ID"
echo "- Group ID: $NEW_GROUP_ID"
```

- [ ] **Step 2: Commit**
```bash
git add k6/refresh-token.sh
git commit -m "test(k6): automatically inject portfolio and group IDs during token refresh"
```
