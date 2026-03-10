# 배포 설정(deploy/) 코드 리뷰 보고서

## 변경 요약
API Blue-Green 슬롯 도입, Kafka/Zookeeper 인프라 강화, n8n 기반의 배포 자동화 스크립트 구축 등 전반적인 운영 환경 개선이 이루어졌습니다.

## 종합 의견
전반적인 Blue-Green 배포 아키텍처 설계는 훌륭합니다. 하지만 헬스체크 포트 불일치, 커스텀 이미지 내 필수 패키지(bash) 누락, 컨테이너 권한 문제 등 **실제 운영 환경에서 배포 실패를 유발할 수 있는 크리티컬한 문제들**이 발견되었습니다. 아래 상세 내용을 확인하여 수정이 필요합니다.

---

## 파일별 점검 결과

### 1. deploy/Dockerfile.n8n
*   **L7: [HIGH] `bash` 패키지 누락**
    *   배포 스크립트(`deploy-api.sh`, `deploy-batch.sh`)는 `#!/usr/bin/env bash`를 사용하며 Bash 전용 문법을 포함하고 있습니다. 하지만 베이스 이미지인 Alpine 기반 n8n에는 bash가 기본 설치되어 있지 않아 스크립트 실행 시 오류가 발생합니다.
    *   **권장 수정:** `apk add bash` 명령어를 추가하여 설치해야 합니다.
*   **L11: [MEDIUM] `docker.sock` 권한 문제**
    *   컨테이너가 `node` 사용자로 실행되는데, 호스트의 `/var/run/docker.sock`은 보통 `root:docker` 권한입니다. 그룹 GID가 맞지 않으면 권한 거부(Permission Denied)로 인해 docker 명령어를 수행할 수 없습니다.

### 2. deploy/docker-compose.prod.yml
*   **L121: [HIGH] `stockwellness-batch` 헬스체크 포트 불일치**
    *   `application-prod.yaml` 설정상 배치는 `8081` 포트를 사용하지만, `docker-compose`의 헬스체크는 `8080` 포트로 요청을 보내고 있습니다. 이로 인해 배포 시 항상 Unhealthy 상태로 인식되어 배포가 실패하게 됩니다.
    *   **권장 수정:** `http://localhost:8081/actuator/health`로 수정이 필요합니다.
*   **L67: [MEDIUM] Zookeeper 헬스체크 `nc` 의존성**
    *   Bitnami Zookeeper 이미지에는 `nc`가 포함되지 않은 경우가 많습니다. `nc`가 없을 경우 헬스체크가 실패하여 Kafka 실행까지 차단될 수 있습니다.
    *   **권장 수정:** `/dev/tcp`를 활용한 Bash 내장 기능을 사용하거나 `zkServer.sh status` 등으로 대체하는 것이 안전합니다.
*   **L142: [LOW] Nginx의 API 슬롯 의존성 강화**
    *   Nginx는 API 슬롯이 완전히 `healthy` 상태가 된 후 시작되는 것이 502 에러 방지에 유리합니다.

### 3. deploy/scripts/deploy-api.sh
*   **L133: [MEDIUM] `blue` 슬롯의 Profile 누락**
    *   스크립트에서 구 슬롯을 정지할 때 `--profile "${CURRENT_SLOT}"`을 사용하지만, `docker-compose.prod.yml`에서 `blue` 슬롯에는 프로파일이 설정되어 있지 않습니다. 이로 인해 `blue` 슬롯 정지 명령이 제대로 작동하지 않을 수 있습니다.
    *   **권장 수정:** `docker-compose.prod.yml`의 `blue` 서비스에도 `profiles: [ blue ]`를 추가해야 합니다.

### 4. deploy/scripts/deploy-batch.sh
*   **L27: [LOW] 로그 언어 혼용**
    *   `deploy-api.sh`는 영어로 로그를 통일했으나, 배치 스크립트에는 한국어 로그가 섞여 있습니다. 일관성을 위해 영어로 통일하는 것을 권장합니다.
