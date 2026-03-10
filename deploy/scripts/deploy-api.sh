#!/usr/bin/env bash
# ================================================================
# StockWellness API — Blue-Green Zero-Downtime Deployment Script
# Path: /home/chan/stockwellness/deploy/scripts/deploy-api.sh
#
# Usage:
#   ./deploy-api.sh <IMAGE_TAG>
#   e.g. ./deploy-api.sh abc1234
#
# Called from n8n Execute Command node:
#   /deploy/scripts/deploy-api.sh {{ $json.body.tag }}
# ================================================================

set -euo pipefail

# ── 환경 변수 로드 ──────────────────────────────────────────
# .env.prod 파일이 있으면 로드 (n8n 등 환경변수가 없는 곳에서 실행 대비)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env.prod"

if [[ -f "${ENV_FILE}" ]]; then
    echo "[$(date '+%H:%M:%S')] .env.prod 파일에서 환경 변수를 로드합니다."
    # 주석 제외하고 변수 추출하여 export
    set -a
    source "${ENV_FILE}"
    set +a
fi

# ── 인자 처리 ────────────────────────────────────────────
IMAGE_TAG="${1:?이미지 태그 인자가 필요합니다. 예: ./deploy-api.sh abc1234}"
REGISTRY="${REGISTRY:-ghcr.io}"
REPO_OWNER="${REPO_OWNER:?REPO_OWNER 환경 변수가 필요합니다}"
IMAGE_API="${REGISTRY}/${REPO_OWNER}/stockwellness-api"

PROJECT_DIR="${DEPLOY_DIR:-/deploy}"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"
NGINX_CONF="${PROJECT_DIR}/nginx/conf.d/app.conf"
SLOT_FILE="${PROJECT_DIR}/.active_slot"
DEPLOY_HISTORY="${PROJECT_DIR}/.deploy_history"

HEALTH_RETRIES=12       # 최대 재시도 횟수
HEALTH_INTERVAL=10      # 체크 간격 (초)
GRACE_PERIOD=30         # 이전 슬롯 중지 전 유예 기간 (초)

# ── 로그 헬퍼 ──────────────────────────────────────────────────
log()  { 
    local msg="[$(date '+%H:%M:%S')] $*"
    echo "${msg}"
}
ok()   { echo "[$(date '+%H:%M:%S')] ✅ $*"; }
warn() { echo "[$(date '+%H:%M:%S')] ⚠️  $*"; }

# ── 오류 진단 수집 ──────────────────────────────────────────────
collect_diagnostics() {
    local service_name="$1"
    local port="$2"
    echo "--- ${service_name} 진단 데이터 수집 ---" >&2
    
    echo "[Docker 로그 - 최근 20줄]" >&2
    docker logs --tail 20 "${service_name}" >&2 || echo "Docker 로그를 가져올 수 없습니다." >&2
    
    echo -e "\n[Spring Boot Actuator 헬스 상태]" >&2
    # 컨테이너 내부에서 localhost로 접근 시도
    docker exec "${service_name}" wget -qO- http://localhost:8080/actuator/health 2>/dev/null >&2 || echo "Actuator 헬스 정보를 가져올 수 없습니다. (포트: 8080)" >&2
    
    echo -e "\n[Nginx 에러 로그 - 최근 10줄]" >&2
    if [[ -f "${PROJECT_DIR}/nginx/logs/error.log" ]]; then
        tail -n 10 "${PROJECT_DIR}/nginx/logs/error.log" >&2
    else
        docker compose exec -T nginx tail -n 10 /var/log/nginx/error.log 2>/dev/null >&2 || echo "Nginx 로그에 접근할 수 없습니다." >&2
    fi
    echo "---------------------------------------" >&2
}

fail() { 
    local error_msg="$*"
    # 인자 중 서비스 이름이 있으면 진단 수집 시도 (여기서는 로직 단순화를 위해 전역 변수 활용 가능)
    if [[ -n "${NEXT_SERVICE:-}" ]]; then
        collect_diagnostics "${NEXT_SERVICE}" "${NEXT_PORT:-8080}"
    fi
    
    echo "[$(date '+%H:%M:%S')] ❌ ${error_msg}" >&2
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [FAIL] API Deployment (${IMAGE_TAG}) - ${error_msg}" >> "${DEPLOY_HISTORY:-/dev/null}"
    exit 1; 
}

# ── 현재 활성 슬롯 읽기 ─────────────────────────────────────
if [[ -f "${SLOT_FILE}" ]]; then
    CURRENT_SLOT=$(cat "${SLOT_FILE}")
else
    CURRENT_SLOT="blue"   # 초기 상태는 blue
fi

# ── 다음 슬롯 결정 ──────────────────────────────────────────
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
log " 현재 슬롯: ${CURRENT_SLOT} -> 다음 슬롯: ${NEXT_SLOT}"
log " 이미지: ${IMAGE_API}:${IMAGE_TAG}"
log "===================================================="

# 배포 시작 이력 기록
echo "[$(date '+%Y-%m-%d %H:%M:%S')] [START] API Deployment (${IMAGE_TAG})" >> "${DEPLOY_HISTORY:-/dev/null}"

cd "${PROJECT_DIR}"

# ── 1단계: 새 슬롯 이미지 가져오기 ──────────────────────────────────
log "[1/5] 이미지 가져오는 중: ${IMAGE_API}:${IMAGE_TAG}"
docker pull "${IMAGE_API}:${IMAGE_TAG}" || fail "이미지 가져오기 실패"

# ── 2단계: 새 슬롯 컨테이너 시작 ────────────────────────────
log "[2/5] 새 슬롯 (${NEXT_SLOT}) 시작 중..."

# .env.prod의 이미지 태그 업데이트
if [[ "${NEXT_SLOT}" == "green" ]]; then
    sed -i "s|^API_TAG_GREEN=.*|API_TAG_GREEN=${IMAGE_TAG}|" .env.prod
else
    sed -i "s|^API_TAG_BLUE=.*|API_TAG_BLUE=${IMAGE_TAG}|" .env.prod
fi

# 새 컨테이너 시작 (green 슬롯은 프로필로 분리됨)
docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
    --profile "${NEXT_SLOT}" \
    up -d --no-deps --force-recreate "${NEXT_SERVICE}"

# ── 3단계: 헬스 체크 ─────────────────────────────────────────
log "[3/5] 헬스 체크 대기 중 (최대 $((HEALTH_RETRIES * HEALTH_INTERVAL))초)..."

for ((i=1; i<=HEALTH_RETRIES; i++)); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "${NEXT_SERVICE}" 2>/dev/null || echo "")
    if [[ "${STATUS}" == "healthy" ]]; then
        ok "헬스 체크 통과 (${i}/${HEALTH_RETRIES})"
        break
    fi
    log "  대기 중... ${i}/${HEALTH_RETRIES} (상태: ${STATUS:-응답 없음})"
    sleep "${HEALTH_INTERVAL}"

    if [[ ${i} -eq ${HEALTH_RETRIES} ]]; then
        warn "헬스 체크 실패 — 롤백 진행"
        docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
            --profile "${NEXT_SLOT}" stop "${NEXT_SERVICE}"
        fail "배포 실패: ${NEXT_SLOT} 슬롯 헬스 체크 시간 초과"
    fi
done

# ── 4단계: nginx 업스트림 전환 ───────────────────────────────
log "[4/5] nginx 업스트림 전환 -> ${NEXT_UPSTREAM}"

sed -i \
    "s|server [^;]*;   # ${CURRENT_MARK}|server ${NEXT_UPSTREAM};   # ${NEXT_MARK}|" \
    "${NGINX_CONF}"

# 설정을 검증한 후 중단 없이 재로드
docker compose --env-file .env.prod -f "${COMPOSE_FILE}" exec -T nginx nginx -t \
    || fail "nginx 설정 검증 실패 — 업스트림 전환 취소"

docker compose --env-file .env.prod -f "${COMPOSE_FILE}" exec -T nginx nginx -s reload
ok "nginx 업스트림 전환 완료"

# ── 5단계: 안정성 모니터링 ────────────────────────────────────────
log "[5/6] 배포 후 안정성 확인 중 (${GRACE_PERIOD}초 대기)..."
sleep "${GRACE_PERIOD}"

# 새 슬롯 상태 최종 확인
FINAL_STATUS=$(docker inspect --format='{{.State.Health.Status}}' "${NEXT_SERVICE}" 2>/dev/null || echo "error")
if [[ "${FINAL_STATUS}" != "healthy" ]]; then
    warn "새 슬롯(${NEXT_SLOT})에서 문제가 감지되었습니다! 롤백을 시작합니다. (상태: ${FINAL_STATUS})"
    
    # Nginx를 다시 이전 슬롯으로 복구
    sed -i \
        "s|server ${NEXT_UPSTREAM};   # ${NEXT_MARK}|server ${CURRENT_SERVICE}:8080;   # ${CURRENT_MARK}|" \
    "${NGINX_CONF}"
    
    docker compose --env-file .env.prod -f "${COMPOSE_FILE}" exec -T nginx nginx -s reload
    warn "nginx 업스트림이 이전 슬롯(${CURRENT_SLOT})으로 복구되었습니다."
    
    # 문제 있는 새 슬롯 중지
    docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
        --profile "${NEXT_SLOT}" stop "${NEXT_SERVICE}"
    
    fail "배포 후 안정성 검증 실패: 시스템이 이전 슬롯으로 자동 롤백되었습니다."
fi

ok "안정성 검증 완료"

# ── 6단계: 이전 슬롯 중지 ────────────────────────────────────────
log "[6/6] 이전 슬롯 (${CURRENT_SLOT}) 중지 중..."

docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
    --profile "${CURRENT_SLOT}" \
    stop "${CURRENT_SERVICE}"

# 활성 슬롯 저장
echo "${NEXT_SLOT}" > "${SLOT_FILE}"

# 배포 이력 기록
echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] API Deployment (${IMAGE_TAG}) - Slot: ${NEXT_SLOT}" >> "${DEPLOY_HISTORY}"

ok "===================================================="
ok " Blue-Green 배포 완료!"
ok " 활성 슬롯: ${NEXT_SLOT}"
ok " 이미지:    ${IMAGE_API}:${IMAGE_TAG}"
ok "===================================================="
