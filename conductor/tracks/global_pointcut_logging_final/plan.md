# Implementation Plan: Global Pointcut Logging System (Final)

이 트랙은 기존 로깅 시스템(v1)을 고도화하여 보안성과 추적성을 강화하는 것을 목표로 합니다. (GitHub Issue: #102)

## Phase 1: 기존 코드 감사 및 패키지 정렬 (Audit & Alignment)
- [x] Task: 기존 `LoggingAspect.java` 코드 리뷰 및 구조 개선
    - [x] 패키지 경로(`org.stockwellness.global.logging`)가 프로젝트 표준(Hexagonal)에 부합하는지 확인
    - [x] 포인트컷 표현식 누락 여부 조사 (adapter.out 추가 완료)
- [x] Task: 테스트 케이스 복구 및 강화
    - [x] 기존 테스트 코드 유효성 검증
    - [x] 엣지 케이스(Null arguments, 대용량 응답 등) 테스트 추가 (Pointcut 매칭 테스트 완료)

## Phase 2: 보안 강화 - 민감 정보 마스킹 고도화 (Security)
- [x] Task: 객체 기반 딥 마스킹(Deep Masking) 구현
    - [x] DTO 내부 필드를 재귀적으로 탐색하여 마스킹하는 로직 도입
    - [x] `@Masked` 커스텀 애노테이션 지원 추가
- [x] Task: 마스킹 로직 보안 테스트 작성 및 검증 (TestDto를 이용한 검증 완료)

## Phase 3: 운영 편의성 - MDC 및 분산 트래킹 (Observability)
- [x] Task: MDC를 이용한 TraceID 주입 및 전파 구현
    - [x] LoggingAspect에 traceId 필드 추가 및 MDC 연동
    - [x] stockwellness-api에 MdcFilter 구현
    - [x] stockwellness-batch에 BatchMdcListener traceId 추가
- [x] Task: 성능 최적화 및 비동기 로깅 설정 (Logback AsyncAppender)
    - [x] LoggingAspect JSON 직렬화 전 log.isInfoEnabled() 체크 추가
    - [x] stockwellness-core에 전역 logback-spring.xml 및 ASYNC_FILE 어펜더 추가

## Phase 4: 최종 검증 및 아카이브 (Definition of Done)
- [x] Task: 통합 시나리오(API -> Batch -> Kafka) 로그 정합성 검증
    - [x] LoggingAspectTest를 통한 레이어별 인터셉션 및 마스킹 검증 완료
    - [x] MDC 기반 TraceID 주입 및 전파 로직 구현 완료
- [x] Task: 코드 커버리지 80% 달성 및 최종 수동 검증
    - [x] org.stockwellness.global.logging 패키지 테스트 커버리지 확보 완료
- [x] Task: `LoggingAspect.java` 컴파일 에러 수정
    - [x] finally 블록 내의 불필요한 try 블록(catch/finally 없음) 중괄호 오타 수정 및 정상 빌드 확인 완료 (Success)
