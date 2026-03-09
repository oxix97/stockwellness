#!/usr/bin/env bash
# ================================================================
# StockWellness Batch — 롤링 재시작 배포 스크립트
# (Batch는 Blue-Green 불필요 — 단순 이미지 교체 후 재시작)
#
# n8n Execute Command 노드에서 호출:
#   /scripts/deploy-batch.sh {{ $json.tag }}
# ================================================================

set -euo pipefail

IMAGE_TAG="${1:?이미지 태그를 인자로 전달하세요}"
REGISTRY="${REGISTRY:-ghcr.io}"
REPO_OWNER="${REPO_OWNER:?환경변수 REPO_OWNER 설정 필요}"
IMAGE_BATCH="${REGISTRY}/${REPO_OWNER}/stockwellness-batch"

PROJECT_DIR="/Volumes/Ubuntu-Home/project/deploy"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"

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

ok "Batch 배포 완료: ${IMAGE_BATCH}:${IMAGE_TAG}"
