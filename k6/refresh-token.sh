#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
COMPOSE_FILE="$SCRIPT_DIR/compose.k6.yaml"

echo "==> [Auth] 인증 토큰 갱신을 시작합니다..."

# 1. k6 login 시나리오 실행 및 JSON 응답 추출
# level=info msg= 내부의 JSON 문자열을 추출하고 이스케이프 해제
RAW_OUTPUT=$(docker compose -f "$COMPOSE_FILE" run --rm k6 run /scripts/scenarios/auth-login.js 2>&1)
TOKEN_JSON=$(echo "$RAW_OUTPUT" | grep 'level=info msg=' | sed 's/.*msg="\(.*\)".*/\1/' | sed 's/\\"/"/g' | head -n 1)

if [ -z "$TOKEN_JSON" ]; then
    echo "ERROR: 토큰 추출에 실패했습니다. 로그를 확인하세요."
    # 디버깅용: 출력 내용 확인
    echo "$RAW_OUTPUT" | grep 'level=info'
    exit 1
fi

# 2. jq를 사용하여 accessToken 필드만 추출
NEW_TOKEN=$(echo "$TOKEN_JSON" | jq -r '.accessToken')

if [ "$NEW_TOKEN" == "null" ] || [ -z "$NEW_TOKEN" ]; then
    echo "ERROR: 유효한 accessToken을 찾을 수 없습니다."
    exit 1
fi

# 3. .env 파일의 ACCESS_TOKEN 값 업데이트
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS sed
    sed -i '' "s|^ACCESS_TOKEN=.*|ACCESS_TOKEN=$NEW_TOKEN|" "$ENV_FILE"
else
    # Linux sed
    sed -i "s|^ACCESS_TOKEN=.*|ACCESS_TOKEN=$NEW_TOKEN|" "$ENV_FILE"
fi

echo "SUCCESS: ACCESS_TOKEN이 성공적으로 갱신되었습니다."
echo "New Token (Prefix): ${NEW_TOKEN:0:20}..."
