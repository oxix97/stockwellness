# 백엔드 아키텍처

## 확인된 모듈 구분

- `stockwellness-core`: 도메인 규칙, 영속성 및 공통 비즈니스 로직
- `stockwellness-api`: HTTP/API 경계
- `stockwellness-batch`: 배치 실행 경계

## 불변 제약

- 의존성: `stockwellness-api`, `stockwellness-batch` → `stockwellness-core`
- `core → api`, `core → batch`, `api ↔ batch` 직접 의존 금지
- 실제 모듈, 기술 버전, 실행 명령은 `settings.gradle.kts`, `build.gradle.kts`, 운영 코드로 검증
- API 요청/응답과 배치 I/O에 핵심 비즈니스 규칙을 숨기지 않는다.

## 변경 검토

동시성, Kafka 메시지, Outbox, Redis 캐시, 외부 KIS API의 실패 경계를 실제 코드와 테스트에서 확인한다. 동작을 추측하지 않는다.

## 상세 구조

[기존 아키텍처 가이드](../documents/architecture.md)와 [개발 가이드](../documents/backend-development-guide.md)를 참고하고 실제 코드와 차이가 있으면 확인한다.
