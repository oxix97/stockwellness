#!/usr/bin/env bash
# ================================================================
# StockWellness Front-End — Blue-Green Zero-Downtime Deployment
# Path: /home/chan/stockwellness-infra/scripts/deploy-front.sh
#
# Usage:
#   ./deploy-front.sh <IMAGE_TAG>
#   e.g. ./deploy-front.sh abc1234
# ================================================================

set -euo pipefail

# ── 환경 변수 로드 ──────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env"

if [[ -f "${ENV_FILE}" ]]; then
    echo "[$(date '+%H:%M:%S')] .env 파일에서 환경 변수를 로드합니다."
    set -a
    source "${ENV_FILE}"
    set +a
fi

# ── 인자 처리 ────────────────────────────────────────────
IMAGE_TAG="${1:?이미지 태그 인자가 필요합니다. 예: ./deploy-front.sh abc1234}"
REGISTRY="${REGISTRY:-ghcr.io}"
REPO_OWNER="${REPO_OWNER:?REPO_OWNER 환경 변수가 필요합니다}"
IMAGE_FRONT="${REGISTRY}/${REPO_OWNER}/stockwellness-front"

PROJECT_DIR="${DEPLOY_DIR:-/home/chan/stockwellness-infra}"
COMPOSE_FILE="${PROJECT_DIR}/compose.yml"
NGINX_CONF="${PROJECT_DIR}/nginx/conf.d/app.conf"
SLOT_FILE="${PROJECT_DIR}/.active_front_slot"
DEPLOY_HISTORY="${PROJECT_DIR}/.deploy_history"

HEALTH_RETRIES=12
HEALTH_INTERVAL=10
GRACE_PERIOD=15

# ── 로그 헬퍼 ──────────────────────────────────────────────────
log()  { echo "[$(date '+%H:%M:%S')] $*"; }
ok()   { echo "[$(date '+%H:%M:%S')] ✅ $*"; }
warn() { echo "[$(date '+%H:%M:%S')] ⚠️  $*"; }

# ── 오류 진단 ──────────────────────────────────────────────────
collect_diagnostics() {
    local service_name="$1"
    echo "--- ${service_name} 진단 데이터 수집 ---" >&2
    docker logs --tail 20 "${service_name}" >&2 || true
    echo "---------------------------------------" >&2
}

fail() { 
    local error_msg="$*"
    if [[ -n "${NEXT_SERVICE:-}" ]]; then
        collect_diagnostics "${NEXT_SERVICE}"
    fi
    echo "[$(date '+%H:%M:%S')] ❌ ${error_msg}" >&2
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [FAIL] Front-End Deployment (${IMAGE_TAG}) - ${error_msg}" >> "${DEPLOY_HISTORY}"
    exit 1; 
}

# ── 현재 활성 슬롯 읽기 ─────────────────────────────────────
if [[ -f "${SLOT_FILE}" ]]; then
    CURRENT_SLOT=$(cat "${SLOT_FILE}")
else
    CURRENT_SLOT="blue"
fi

# ── 다음 슬롯 결정 ──────────────────────────────────────────
if [[ "${CURRENT_SLOT}" == "blue" ]]; then
    NEXT_SLOT="green"
    NEXT_PORT="8085"
    NEXT_SERVICE="stockwellness-front-green"
    CURRENT_SERVICE="stockwellness-front-blue"
else
    NEXT_SLOT="blue"
    NEXT_PORT="8083"
    NEXT_SERVICE="stockwellness-front-blue"
    CURRENT_SERVICE="stockwellness-front-green"
fi

log "🚀 Front-End 배포를 시작합니다: ${CURRENT_SLOT} -> ${NEXT_SLOT} (${IMAGE_TAG})"

# ── 1. 새 이미지 가져오기 및 컨테이너 시작 ───────────────────
log "새 이미지를 준비 중입니다: ${IMAGE_FRONT}:${IMAGE_TAG}"

# docker-compose 환경변수 설정 (실행 시점에만 적용)
export FRONT_TAG_${NEXT_SLOT^^}="${IMAGE_TAG}"
# .env의 이미지 태그 영구 업데이트 (재시작 시 올바른 이미지 사용 보장)
sed -i "s|^FRONT_TAG_${NEXT_SLOT^^}=.*|FRONT_TAG_${NEXT_SLOT^^}=${IMAGE_TAG}|" "${ENV_FILE}"

log "${NEXT_SERVICE} 서비스를 시작합니다 (Port: ${NEXT_PORT})..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" --profile front-${NEXT_SLOT} up -d "${NEXT_SERVICE}"

# ── 2. 헬스 체크 ───────────────────────────────────────────
# docker inspect 사용 (n8n executeCommand는 컨테이너 내부에서 실행되므로
# localhost:포트 접근 불가 — compose healthcheck 결과를 직접 확인)
log "서비스 안정화 대기 중 (헬스 체크, 최대 $((HEALTH_RETRIES * HEALTH_INTERVAL))초)..."

for ((i=1; i<=HEALTH_RETRIES; i++)); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "${NEXT_SERVICE}" 2>/dev/null || echo "")
    if [[ "${STATUS}" == "healthy" ]]; then
        ok "헬스 체크 통과 (${i}/${HEALTH_RETRIES})"
        break
    fi
    log "  대기 중... ${i}/${HEALTH_RETRIES} (상태: ${STATUS:-응답 없음})"
    sleep "${HEALTH_INTERVAL}"

    if [[ ${i} -eq ${HEALTH_RETRIES} ]]; then
        fail "새 서비스(${NEXT_SERVICE}) 헬스 체크 시간 초과"
    fi
done

# ── 3. Nginx 설정 교체 (Upstream Switch) ───────────────────
log "Nginx 라우팅을 ${NEXT_SLOT} 슬롯으로 전환합니다..."

# upstream 블록 내의 서버 주소를 교체 (주석 무관하게 패턴 매칭)
# server stockwellness-front-(blue|green):80; 패턴을 찾아 교체
sed -i "s|set \$front_upstream stockwellness-front-[a-z]*:80;|set \$front_upstream stockwellness-front-${NEXT_SLOT}:80;|g" "${NGINX_CONF}"

# Nginx 설정 테스트 및 리로드
if docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T nginx nginx -t > /dev/null 2>&1; then
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T nginx nginx -s reload
    ok "Nginx 설정이 리로드되었습니다."
else
    # 실패 시 원복 (이전 슬롯으로 다시 되돌림)
    sed -i "s|set \$front_upstream stockwellness-front-${NEXT_SLOT}:80;|set \$front_upstream stockwellness-front-${CURRENT_SLOT}:80;|g" "${NGINX_CONF}"
    fail "Nginx 설정 테스트 실패로 전환이 취소되었습니다."
fi

# ── 4. 상태 업데이트 및 이전 슬롯 정리 ───────────────────────
echo "${NEXT_SLOT}" > "${SLOT_FILE}"

log "이전 슬롯(${CURRENT_SERVICE})을 정리하기 전 유예 기간(${GRACE_PERIOD}s)을 가집니다..."
sleep "${GRACE_PERIOD}"

log "이전 슬롯(${CURRENT_SERVICE})을 중지합니다..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" stop "${CURRENT_SERVICE}" || warn "이전 슬롯 중지 중 오류 발생 (이미 중지되었을 수 있음)"

ok "배포 완료: ${IMAGE_TAG}"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] Front-End Deployment (${IMAGE_TAG}) - Slot: ${NEXT_SLOT}" >> "${DEPLOY_HISTORY}"
