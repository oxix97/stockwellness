#!/usr/bin/env bash
# ================================================================
# StockWellness API — Blue-Green 무중단 배포 스크립트
# 경로: /Volumes/Ubuntu-Home/project/deploy/scripts/deploy-api.sh
#
# 사용법:
#   ./deploy-api.sh <IMAGE_TAG>
#   예) ./deploy-api.sh abc1234
#
# n8n Execute Command 노드에서 호출:
#   /scripts/deploy-api.sh {{ $json.tag }}
# ================================================================

set -euo pipefail

# ── 인자 처리 ────────────────────────────────────────────────────
IMAGE_TAG="${1:?이미지 태그를 인자로 전달하세요. 예: ./deploy-api.sh abc1234}"
REGISTRY="${REGISTRY:-ghcr.io}"
REPO_OWNER="${REPO_OWNER:?환경변수 REPO_OWNER 설정 필요}"
IMAGE_API="${REGISTRY}/${REPO_OWNER}/stockwellness-api"

PROJECT_DIR="/Volumes/Ubuntu-Home/project/deploy"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"
NGINX_CONF="${PROJECT_DIR}/nginx/conf.d/app.conf"
SLOT_FILE="${PROJECT_DIR}/.active_slot"

HEALTH_RETRIES=12       # 최대 대기 횟수
HEALTH_INTERVAL=10      # 체크 간격(초)
GRACE_PERIOD=30         # 구 슬롯 종료 대기(초)

# ── 색상 출력 헬퍼 ───────────────────────────────────────────────
log()  { echo "[$(date '+%H:%M:%S')] $*"; }
ok()   { echo "[$(date '+%H:%M:%S')] ✅ $*"; }
warn() { echo "[$(date '+%H:%M:%S')] ⚠️  $*"; }
fail() { echo "[$(date '+%H:%M:%S')] ❌ $*" >&2; exit 1; }

# ── 현재 활성 슬롯 읽기 ─────────────────────────────────────────
if [[ -f "${SLOT_FILE}" ]]; then
    CURRENT_SLOT=$(cat "${SLOT_FILE}")
else
    CURRENT_SLOT="blue"   # 초기 상태는 blue
fi

# ── 슬롯 전환 결정 ─────────────────────────────────────────────
if [[ "${CURRENT_SLOT}" == "blue" ]]; then
    NEXT_SLOT="green"
    NEXT_PORT="8082"
    NEXT_SERVICE="stockwellness-api-green"
    NEXT_UPSTREAM="stockwellness-api-green:8080"
    CURRENT_SERVICE="stockwellness-api-blue"
    CURRENT_MARK="BLUE_ACTIVE"
    NEXT_MARK="GREEN_ACTIVE"
else
    NEXT_SLOT="blue"
    NEXT_PORT="8080"
    NEXT_SERVICE="stockwellness-api-blue"
    NEXT_UPSTREAM="stockwellness-api-blue:8080"
    CURRENT_SERVICE="stockwellness-api-green"
    CURRENT_MARK="GREEN_ACTIVE"
    NEXT_MARK="BLUE_ACTIVE"
fi

log "===================================================="
log " Blue-Green API 배포 시작"
log " 현재 슬롯: ${CURRENT_SLOT} → 신규 슬롯: ${NEXT_SLOT}"
log " 배포 이미지: ${IMAGE_API}:${IMAGE_TAG}"
log "===================================================="

cd "${PROJECT_DIR}"

# ── STEP 1: 신규 슬롯 이미지 Pull ─────────────────────────────
log "[1/5] 신규 이미지 Pull: ${IMAGE_API}:${IMAGE_TAG}"
docker pull "${IMAGE_API}:${IMAGE_TAG}" || fail "이미지 Pull 실패"

# ── STEP 2: 신규 슬롯 컨테이너 교체 기동 ──────────────────────
log "[2/5] 신규 슬롯(${NEXT_SLOT}) 기동 중..."

# .env.prod에 신규 슬롯 태그 갱신
if [[ "${NEXT_SLOT}" == "green" ]]; then
    sed -i "s|^API_TAG_GREEN=.*|API_TAG_GREEN=${IMAGE_TAG}|" .env.prod
else
    sed -i "s|^API_TAG_BLUE=.*|API_TAG_BLUE=${IMAGE_TAG}|" .env.prod
fi

# green 슬롯은 profile로 분리 — 신규 컨테이너 시작
docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
    --profile "${NEXT_SLOT}" \
    up -d --no-deps --force-recreate "${NEXT_SERVICE}"

# ── STEP 3: Health Check ────────────────────────────────────────
log "[3/5] Health Check 대기 (최대 $((HEALTH_RETRIES * HEALTH_INTERVAL))초)..."
HEALTH_URL="http://localhost:${NEXT_PORT}/actuator/health"

for ((i=1; i<=HEALTH_RETRIES; i++)); do
    STATUS=$(curl -sf "${HEALTH_URL}" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")
    if [[ "${STATUS}" == "UP" ]]; then
        ok "Health Check 통과 (${i}/${HEALTH_RETRIES}회)"
        break
    fi
    log "  대기 중... ${i}/${HEALTH_RETRIES} (상태: ${STATUS:-응답없음})"
    sleep "${HEALTH_INTERVAL}"

    if [[ ${i} -eq ${HEALTH_RETRIES} ]]; then
        warn "Health Check 실패 — 롤백합니다"
        docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
            --profile "${NEXT_SLOT}" stop "${NEXT_SERVICE}"
        fail "배포 실패: ${NEXT_SLOT} 슬롯 Health Check 시간 초과"
    fi
done

# ── STEP 4: nginx upstream 전환 ────────────────────────────────
log "[4/5] nginx upstream 전환 → ${NEXT_UPSTREAM}"

sed -i \
    "s|server [^;]*;   # ${CURRENT_MARK}|server ${NEXT_UPSTREAM};   # ${NEXT_MARK}|" \
    "${NGINX_CONF}"

# nginx 설정 검증 후 무중단 리로드
docker compose -f "${COMPOSE_FILE}" exec -T nginx nginx -t \
    || fail "nginx 설정 검증 실패 — upstream 전환 취소"

docker compose -f "${COMPOSE_FILE}" exec -T nginx nginx -s reload
ok "nginx upstream 전환 완료"

# ── STEP 5: 구 슬롯 종료 ────────────────────────────────────────
log "[5/5] 구 슬롯(${CURRENT_SLOT}) ${GRACE_PERIOD}초 후 종료..."
sleep "${GRACE_PERIOD}"

docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
    stop "${CURRENT_SERVICE}"

# 활성 슬롯 파일 갱신
echo "${NEXT_SLOT}" > "${SLOT_FILE}"

ok "===================================================="
ok " Blue-Green 배포 완료!"
ok " 활성 슬롯: ${NEXT_SLOT}"
ok " 이미지:    ${IMAGE_API}:${IMAGE_TAG}"
ok "===================================================="
