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
NEW_SECTOR_CODE=$(echo "$TOKEN_JSON" | jq -r '.sectorCode // empty')

if [ -z "$NEW_TOKEN" ]; then
    echo "ERROR: 유효한 accessToken을 찾을 수 없습니다."
    exit 1
fi

# 3. .env 파일 업데이트 (macOS 및 Linux 호환)
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s|^ACCESS_TOKEN=.*|ACCESS_TOKEN=$NEW_TOKEN|" "$ENV_FILE"
    [[ -n "$NEW_PF_ID" ]] && sed -i '' "s|^PORTFOLIO_ID=.*|PORTFOLIO_ID=$NEW_PF_ID|" "$ENV_FILE"
    [[ -n "$NEW_GROUP_ID" ]] && sed -i '' "s|^GROUP_ID=.*|GROUP_ID=$NEW_GROUP_ID|" "$ENV_FILE"
    [[ -n "$NEW_SECTOR_CODE" ]] && sed -i '' "s|^SECTOR_CODE=.*|SECTOR_CODE=$NEW_SECTOR_CODE|" "$ENV_FILE"
else
    sed -i "s|^ACCESS_TOKEN=.*|ACCESS_TOKEN=$NEW_TOKEN|" "$ENV_FILE"
    [[ -n "$NEW_PF_ID" ]] && sed -i "s|^PORTFOLIO_ID=.*|PORTFOLIO_ID=$NEW_PF_ID|" "$ENV_FILE"
    [[ -n "$NEW_GROUP_ID" ]] && sed -i "s|^GROUP_ID=.*|GROUP_ID=$NEW_GROUP_ID|" "$ENV_FILE"
    [[ -n "$NEW_SECTOR_CODE" ]] && sed -i "s|^SECTOR_CODE=.*|SECTOR_CODE=$NEW_SECTOR_CODE|" "$ENV_FILE"
fi

echo "SUCCESS: .env 파일이 성공적으로 갱신되었습니다."
echo "- Token (Prefix): ${NEW_TOKEN:0:20}..."
echo "- Portfolio ID: $NEW_PF_ID"
echo "- Group ID: $NEW_GROUP_ID"
echo "- Sector Code: $NEW_SECTOR_CODE"
