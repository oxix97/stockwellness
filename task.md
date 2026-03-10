# 배포 환경 개선 태스크 (Task List)

`report.md`의 리뷰 결과를 바탕으로 배포 안정성 및 정합성을 높이기 위한 수정 계획입니다.

## 1단계: Docker 이미지 및 권한 설정 (`deploy/Dockerfile.n8n`)
- [ ] **Bash 패키지 설치**: Alpine 베이스 이미지에 `bash`가 없으므로 `apk add --no-cache bash` 추가.
- [ ] **Docker Socket 권한 가이드 확인**: `node` 사용자가 `/var/run/docker.sock`에 접근할 수 있도록 권한 설정 검토 (GID 매칭 등).

## 2단계: Docker Compose 설정 최적화 (`deploy/docker-compose.prod.yml`)
- [ ] **Zookeeper 헬스체크 개선**: `nc` 의존성을 제거하고 `zkServer.sh status` 또는 `/dev/tcp` 소켓 체크로 변경.
- [ ] **API Blue 슬롯 프로파일 추가**: `deploy-api.sh`와의 정합성을 위해 `stockwellness-api-blue` 서비스에 `profiles: [ blue ]` 추가.
- [ ] **Batch 헬스체크 포트 수정**: `8080`으로 잘못 설정된 포트를 실제 구동 포트인 `8081`로 수정.
- [ ] **Nginx 의존성 조건 변경**: `depends_on` 대상을 `service_healthy` 상태로 강화.

## 3단계: 배포 스크립트 로그 일관성 및 안정화 (`deploy/scripts/`)
- [ ] **Batch 로그 영문 통일**: `deploy-batch.sh` 내부에 남은 한국어 로그 메시지를 모두 영문으로 교체하여 `deploy-api.sh`와 일관성 유지.
- [ ] **Nginx 리로드 로직 검증**: `deploy-api.sh`에서 `nginx -t` 검증 시 환경변수(`.env.prod`)가 누락되지 않았는지 재확인.

## 4단계: 최종 검증
- [ ] **로컬 시뮬레이션**: 수정된 `docker-compose` 설정 파일 문법 검토 (`docker compose config`).
- [ ] **스크립트 구동 테스트**: `bash -n` 등을 이용한 스크립트 문법 오류 체크.
