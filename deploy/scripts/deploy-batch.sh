#!/usr/bin/env bash
# ================================================================
# StockWellness Batch — 롤링 재시작 배포 스크립트
# (배치는 Blue-Green이 필요하지 않음 — 단순 이미지 교체)
#
# n8n Execute Command 노드에서 호출됨:
#   /deploy/scripts/deploy-batch.sh {{ $json.body.tag }}
# ================================================================

set -euo pipefail

# ── 환경 변수 로드 ──────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env.prod"

if [[ -f "${ENV_FILE}" ]]; then
    echo "[$(date '+%H:%M:%S')] .env.prod 파일에서 환경 변수를 로드합니다."
    set -a
    source "${ENV_FILE}"
    set +a
fi

IMAGE_TAG="${1:?이미지 태그 인자가 필요합니다}"
REGISTRY="${REGISTRY:-ghcr.io}"
REPO_OWNER="${REPO_OWNER:?REPO_OWNER 환경 변수가 필요합니다}"
IMAGE_BATCH="${REGISTRY}/${REPO_OWNER}/stockwellness-batch"

PROJECT_DIR="${DEPLOY_DIR:-/deploy}"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"
DEPLOY_HISTORY="${PROJECT_DIR}/.deploy_history"

HEALTH_RETRIES=12
HEALTH_INTERVAL=10

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
ok()   { echo "[$(date '+%H:%M:%S')] ✅ $*"; }

collect_diagnostics() {
    local service_name="stockwellness-batch"
    echo "--- ${service_name} 진단 데이터 수집 ---" >&2
    echo "[Docker 로그 - 최근 20줄]" >&2
    docker logs --tail 20 "${service_name}" >&2 || echo "Docker 로그를 가져올 수 없습니다." >&2
    echo -e "\n[Spring Boot Actuator 헬스 상태]" >&2
    docker exec "${service_name}" wget -qO- http://localhost:8080/actuator/health 2>/dev/null >&2 || echo "Actuator 헬스 정보를 가져올 수 없습니다. (포트: 8080)" >&2
    echo "---------------------------------------" >&2
}

fail() { 
    collect_diagnostics
    echo "[$(date '+%H:%M:%S')] ❌ $*" >&2
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [FAIL] Batch Deployment (${IMAGE_TAG}) - $*" >> "${DEPLOY_HISTORY:-/dev/null}"
    exit 1; 
}

log "배치 배포 시작됨: ${IMAGE_BATCH}:${IMAGE_TAG}"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] [START] Batch Deployment (${IMAGE_TAG})" >> "${DEPLOY_HISTORY:-/dev/null}"

cd "${PROJECT_DIR}"

# 이미지 가져오기
docker pull "${IMAGE_BATCH}:${IMAGE_TAG}" || fail "배치 이미지 가져오기 실패"

# .env.prod의 이미지 태그 업데이트
sed -i "s|^BATCH_TAG=.*|BATCH_TAG=${IMAGE_TAG}|" .env.prod

# 컨테이너 교체
docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
    up -d --no-deps --force-recreate stockwellness-batch

# 헬스 체크
log "헬스 체크 대기 중 (최대 $((HEALTH_RETRIES * HEALTH_INTERVAL))초)..."

for ((i=1; i<=HEALTH_RETRIES; i++)); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' stockwellness-batch 2>/dev/null || echo "")
    if [[ "${STATUS}" == "healthy" ]]; then
        ok "헬스 체크 통과 (${i}/${HEALTH_RETRIES})"
        break
    fi
    log "  대기 중... ${i}/${HEALTH_RETRIES} (상태: ${STATUS:-응답 없음})"
    sleep "${HEALTH_INTERVAL}"

    if [[ ${i} -eq ${HEALTH_RETRIES} ]]; then
        fail "배치 배포 실패: 헬스 체크 시간 초과 (${IMAGE_BATCH}:${IMAGE_TAG})"
    fi
done

echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] Batch Deployment (${IMAGE_TAG})" >> "${DEPLOY_HISTORY:-/dev/null}"
ok "배치 배포 완료: ${IMAGE_BATCH}:${IMAGE_TAG}"
