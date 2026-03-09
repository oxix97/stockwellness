#!/usr/bin/env bash
# ================================================================
# StockWellness Batch — 롤링 재시작 배포 스크립트
# (Batch는 Blue-Green 불필요 — 단순 이미지 교체 후 재시작)
#
# n8n Execute Command 노드에서 호출:
#   /deploy/scripts/deploy-batch.sh {{ $json.tag }}
# ================================================================

set -euo pipefail

IMAGE_TAG="${1:?이미지 태그를 인자로 전달하세요}"
REGISTRY="${REGISTRY:-ghcr.io}"
REPO_OWNER="${REPO_OWNER:?환경변수 REPO_OWNER 설정 필요}"
IMAGE_BATCH="${REGISTRY}/${REPO_OWNER}/stockwellness-batch"

PROJECT_DIR="${DEPLOY_DIR:-/deploy}"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"

HEALTH_RETRIES=12
HEALTH_INTERVAL=10
BATCH_PORT=8081

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
ok()   { echo "[$(date '+%H:%M:%S')] ✅ $*"; }
fail() { echo "[$(date '+%H:%M:%S')] ❌ $*" >&2; exit 1; }

log "Batch 배포 시작: ${IMAGE_BATCH}:${IMAGE_TAG}"

cd "${PROJECT_DIR}"

# 이미지 Pull
docker pull "${IMAGE_BATCH}:${IMAGE_TAG}" || fail "Batch 이미지 Pull 실패"

# .env.prod 태그 갱신
sed -i "s|^BATCH_TAG=.*|BATCH_TAG=${IMAGE_TAG}|" .env.prod

# 컨테이너 교체 재시작
docker compose --env-file .env.prod -f "${COMPOSE_FILE}" \
    up -d --no-deps --force-recreate stockwellness-batch

# Health Check
log "Health Check 대기 (최대 $((HEALTH_RETRIES * HEALTH_INTERVAL))초)..."
HEALTH_URL="http://localhost:${BATCH_PORT}/actuator/health"

for ((i=1; i<=HEALTH_RETRIES; i++)); do
    STATUS=$(curl -sf "${HEALTH_URL}" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")
    if [[ "${STATUS}" == "UP" ]]; then
        ok "Health Check 통과 (${i}/${HEALTH_RETRIES}회)"
        break
    fi
    log "  대기 중... ${i}/${HEALTH_RETRIES} (상태: ${STATUS:-응답없음})"
    sleep "${HEALTH_INTERVAL}"

    if [[ ${i} -eq ${HEALTH_RETRIES} ]]; then
        fail "Batch 배포 실패: Health Check 시간 초과 (${IMAGE_BATCH}:${IMAGE_TAG})"
    fi
done

ok "Batch 배포 완료: ${IMAGE_BATCH}:${IMAGE_TAG}"
