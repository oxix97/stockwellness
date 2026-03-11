# 실행 계획: 포트폴리오 AI 리밸런싱 어드바이저

이 계획은 사용자의 보유 종목, 시장 벤치마크, 기술적 지표를 분석하여 주기적인 리밸런싱 권고안을 제공하는 AI 어드바이저 기능을 구현합니다.

## 1단계: 도메인 모델 및 데이터 접근 계층 (Core/Persistence) [COMPLETED]
핵심 도메인 엔티티와 분석에 필요한 데이터 접근 포트를 정의합니다.

- [x] Task: 어드바이저 보고서 도메인 모델 및 DTO 정의
    - [x] `AdvisorReport`, `AdviceAction` 엔티티 정의
    - [x] `AdviceResponse` DTO 정의 (텍스트 보고서 포함)
- [x] Task: 포트폴리오 데이터 조회 포트 및 어댑터 구현
    - [x] TDD: 현재 보유 종목(티커, 수량, 가치) 조회 로직 테스트 작성
    - [x] QueryDSL을 활용한 포트폴리오 현황 조회 어댑터 구현
- [x] Task: 시장 및 기술적 지표 데이터 조회 포트 구현
    - [x] TDD: 벤치마크 및 종목별 기술적 지표(RSI, MA) 조회 테스트 작성
    - [x] 기존 `StockPrice` 및 `TechnicalIndicator` 저장소 연동 어댑터 구현
- [x] Task: Conductor - User Manual Verification '1단계: 도메인 모델 및 데이터 접근 계층' (Protocol in workflow.md)

## 2단계: AI 엔진 통합 (Spring AI) [COMPLETED]
AI 에이전트를 구성하고 리밸런싱 논리를 구현합니다.

- [x] Task: AI 프롬프트 템플릿 및 인터페이스 정의
    - [x] 리밸런싱 전략(가중치, 위험 관리, 기술적 분석)을 포함한 프롬프트 작성
    - [x] `AiAdvisorPort` 인터페이스 정의
- [x] Task: AI 어드바이저 서비스 구현 (Spring AI/OpenAI)
    - [x] OpenAI API 연동 및 프롬프트 실행 로직 구현
    - [x] AI 응답 파싱 및 보고서 객체 매핑 구현
- [x] Task: TDD - AI 로직 및 추론 검증
    - [x] Mock AI 응답을 활용한 어드바이저 논리 단위 테스트 작성
    - [x] 다양한 시장 상황(과매수, 목표 비중 이탈 등)에 따른 권고 결과 검증
- [x] Task: Conductor - User Manual Verification '2단계: AI 엔진 통합' (Protocol in workflow.md)

## 3단계: 스케줄링 및 오케스트레이션 [COMPLETED]
주기적으로 분석을 실행하고 결과를 저장하는 흐름을 관리합니다.

- [x] Task: 어드바이저 오케스트레이션 서비스 구현
    - [x] TDD: (보유 종목 + 시장 데이터) -> AI -> 저장 프로세스 테스트 작성
    - [x] `AdvisorOrchestrator` 구현 (데이터 수집 및 AI 호출 제어)
- [x] Task: Spring Scheduler를 활용한 주기적 트리거 설정
    - [x] `@Scheduled`를 활용한 주간/월간 실행 로직 구현
    - [x] 실행 이력 및 상태 로깅 구현
- [x] Task: 생성된 보고서 저장 로직 구현
    - [x] PostgreSQL을 활용한 보고서 영속화 구현
- [x] Task: Conductor - User Manual Verification '3단계: 스케줄링 및 오케스트레이션' (Protocol in workflow.md)

## 4단계: API 및 문서화 [COMPLETED]
사용자가 보고서를 조회할 수 있도록 인터페이스를 제공합니다.

- [x] Task: 최신 어드바이저 보고서 조회 API 엔드포인트 구현
    - [x] `GET /api/v1/portfolio/advice/latest` 구현
- [x] Task: Spring REST Docs 및 API 문서 업데이트
    - [x] API 스펙 테스트 작성 및 문서 생성
- [x] Task: 통합 테스트 (E2E 흐름 검증)
    - [x] 데이터베이스부터 AI 호출, 보고서 생성 및 조회까지의 전체 흐름 테스트
- [x] Task: Conductor - User Manual Verification '4단계: API 및 문서화' (Protocol in workflow.md)

## 5단계: 성능 및 최종 검증 [COMPLETED]
시스템 성능을 측정하고 코드를 정리합니다.

- [x] Task: 성능 부하 테스트
    - [x] AI 응답 시간 및 다수 사용자 요청 시의 동시성 처리 확인
- [x] Task: 최종 검증 및 리팩토링
    - [x] 코드 스타일 가이드 준수 확인 및 중복 로직 제거
- [x] Task: Conductor - User Manual Verification '5단계: 성능 및 최종 검증' (Protocol in workflow.md)
