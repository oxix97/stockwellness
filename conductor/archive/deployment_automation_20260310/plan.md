# Implementation Plan: n8n 기반 배포 자동화 및 Slack 상세 알림 시스템

## Phase 1: 배포 스크립트 안정성 및 이력 관리 고도화
- [x] Task: `deploy-api.sh` 수정 - Nginx 전환 후 유예 기간(Grace Period) 동안 헬스체크 재검증 로직 추가
- [x] Task: `deploy-api.sh` 수정 - 검증 실패 시 이전 슬롯으로 자동 롤백 및 에러 로그 기록 로직 구현
- [x] Task: `deploy-api.sh`, `deploy-batch.sh` 수정 - 배포 이력(`.deploy_history`) 기록 기능 추가 (시작, 결과, 태그)
- [x] Task: Conductor - User Manual Verification 'Phase 1 Completion' (Protocol in workflow.md)

## Phase 2: 오류 진단 및 리포팅 로직 강화
- [x] Task: `deploy-api.sh` 수정 - 실패(Unhealthy) 시 `docker logs`, `actuator/health` 데이터를 임시 파일로 수집하는 유틸리티 함수 추가
- [x] Task: Nginx 설정 확인 - 배포 스크립트에서 Nginx 에러 로그(`nginx/error.log`) 접근 권한 및 수집 가능 여부 확인
- [x] Task: 수집된 진단 데이터를 표준 에러(stderr)로 출력하여 n8n에서 캡처 가능하도록 정비
- [x] Task: Conductor - User Manual Verification 'Phase 2 Completion' (Protocol in workflow.md)

## Phase 3: n8n 워크플로우 및 Slack 알림 연동
- [x] Task: n8n 워크플로우 설계 - GitHub Webhook 트리거 및 배포 파라미터(Tag, Service) 파싱 노드 구축 (가이드 완료)
- [x] Task: n8n 워크플로우 설계 - 배포 스크립트 실행(Execute Command) 및 표준 출력/에러 캡처 노드 연결 (가이드 완료)
- [x] Task: Slack 알림 연동 - 성공 시 간결한 메시지 송신 (Block Kit 활용 권장)
- [x] Task: Slack 알림 연동 - 실패 시 상세 진단 데이터(Phase 2 결과물)를 포함한 경고 메시지 송신
- [x] Task: Conductor - User Manual Verification 'Phase 3 Completion' (Protocol in workflow.md)

## Phase 4: 최종 테스트 및 최적화
- [x] Task: 시나리오 테스트 1 - 정상 배포 성공 시 Slack 알림 및 슬롯 전환 확인
- [x] Task: 시나리오 테스트 2 - 의도적 배포 실패 유도 후 자동 롤백 및 Slack 상세 오류 리포트 확인
- [x] Task: 배포 이력 파일(`.deploy_history`) 기록 상태 최종 점검
- [x] Task: Conductor - User Manual Verification 'Phase 4 Completion' (Protocol in workflow.md)
