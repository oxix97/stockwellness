# Track: n8n 기반 안정적인 배포 자동화 및 Slack 상세 알림 시스템 구축

## 1. 개요 (Overview)
본 트랙은 기존 `@deploy/` 하위의 배포 파일들을 바탕으로, n8n과 연동하여 **배포 성공률 100% 지향** 및 **배포 가시성 확보**를 목표로 합니다. 특히 Nginx 전환 이후의 안정성 검증(Post-Switch Monitoring)과 상세한 오류 원인 리포팅을 구현하여 서버 운영 효율을 극대화합니다.

## 2. 기능적 요구사항 (Functional Requirements)
- **n8n 배포 워크플로우 설계**:
  - GitHub Actions로부터 Webhook 트리거 수신 (Image Tag 등 파라미터 포함).
  - API Blue/Green 배포 및 Batch 단순 재시작 배포를 상황에 맞게 실행.
  - 배포 결과(성공/실패)에 따른 조건부 Slack 메시지 발송.
- **안정성 모니터링 (Post-Switch Monitoring)**:
  - Nginx 업스트림 전환 후, 지정된 유예 기간(Grace Period) 동안 새 컨테이너의 헬스체크 재검증.
  - 문제가 발생할 경우 즉시 Nginx를 이전 슬롯으로 되돌리고 새 컨테이너를 중지(Rollback)하는 로직 구현.
- **상세 오류 수집 (Diagnostic Collection)**:
  - 배포 실패 시 `docker logs`, `actuator/health` 상세 JSON, `nginx/error.log` 등을 취합하여 Slack 알림 본문에 포함.
- **배포 이력 관리 (History Logging)**:
  - 배포 시작 시점, 완료 시점, 결과(성공/실패/롤백), 사용된 이미지 태그 정보를 별도 파일(`.deploy_history`)에 기록.

## 3. 비기능적 요구사항 (Non-Functional Requirements)
- **무중단성**: API 배포 시 사용자에게 응답 오류(5xx)가 발생하지 않도록 롤백 시점을 정교하게 제어.
- **사용자 경험**: Slack 메시지는 가독성 좋은 Block Kit 형식을 활용.
- **환경성**: Ubuntu 24.04 환경의 Docker Compose/CLI를 완벽하게 활용.

## 4. 인수 기준 (Acceptance Criteria)
- [ ] n8n Webhook 호출 시 배포 스크립트가 인자값을 정상적으로 전달받아 실행되는가?
- [ ] API 배포 시 Nginx 전환 후 유예 기간 동안 오류가 감지되면 자동으로 이전 슬롯으로 롤백되는가?
- [ ] Slack 알림에 배포 결과와 함께 상세 진단 데이터(로그 등)가 포함되어 전달되는가?
- [ ] `.deploy_history` 파일에 배포 이력이 정확히 기록되는가?

## 5. 제외 범위 (Out of Scope)
- 인프라 하드웨어 설정 (CPU/RAM 등).
- n8n 서비스의 직접적인 외부 인터넷 노출 (Tailscale 사용 전제).
