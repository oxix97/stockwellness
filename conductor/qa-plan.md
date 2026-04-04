# Stockwellness & Stockwellness-Front 통합 QA 계획서

본 문서는 Stockwellness(백엔드) 및 Stockwellness-Front(프론트엔드) 프로젝트의 통합 품질 보증(QA) 계획을 수립합니다. 특히 API 호출과 데이터 정합성에 중점을 두고 검증합니다.

## 1. 테스트 목표
- 프론트엔드와 백엔드 간의 API 통신이 요구사항 명세(OpenAPI/Swagger)와 일치하는지 확인.
- 데이터베이스(PostgreSQL), 캐시(Redis), 메시지 브로커(Kafka) 간의 데이터 흐름 및 정합성 보장.
- 트랜잭셔널 아웃박스(Transactional Outbox) 패턴 및 배치 처리가 안정적으로 동작하여 데이터 유실이 없는지 검증.
- 핵심 비즈니스 로직(포트폴리오 분석, 백테스트, AI 어드바이저)의 결과값이 정확한지 확인.

## 2. 테스트 환경
- **Backend:** `stockwellness-api` (Port: 8080), `stockwellness-batch` (Port: 8081)
- **Frontend:** `stockwellness-front` (로컬 개발 서버 또는 스테이징 환경)
- **Infra:** Docker Compose (PostgreSQL, Redis, Kafka, Zookeeper)
- **External APIs:** KIS API (Mocking 또는 개발계 연동), OpenAI (테스트 모델 활용)

## 3. 핵심 검토 항목 및 테스트 케이스

### 3.1. API 호출 및 프론트엔드 연동 테스트
프론트엔드 컴포넌트에서 백엔드 API를 올바르게 호출하고, 응답 데이터를 적절히 렌더링하는지 검증합니다.

*   **인증 및 인가 (Auth Component)**
    *   [ ] OAuth2 소셜 로그인(카카오, 구글) 성공 및 JWT(Access/Refresh) 토큰 발급 확인.
    *   [ ] 프론트엔드에서 Access Token을 헤더에 포함하여 인증된 API 호출 (예: `/api/v1/members/me`).
    *   [ ] Access Token 만료 시 Refresh Token을 이용한 자동 재발급(`/api/v1/auth/reissue`) 및 프론트엔드 재시도 로직 검증.
    *   [ ] 로그아웃(`/api/v1/auth/logout`) 시 토큰 무효화 및 프론트엔드 상태 초기화 확인.

*   **포트폴리오 관리 (Portfolio Component)**
    *   [ ] 포트폴리오 생성/조회/수정/삭제 (CRUD) 시 API 호출 및 화면 갱신 확인.
    *   [ ] 포트폴리오 건강 진단(`/api/v1/portfolios/{id}/health`) API 호출 시 MDD, Sharpe, Beta 등 위험 지표의 정확성 검증 및 레이더 차트 렌더링.
    *   [ ] 분석 요청 시 `PortfolioAnalysisCompleted` 이벤트가 정상 처리되어 프론트엔드에 상태가 반영되는지 확인.

*   **AI 리밸런싱 어드바이저**
    *   [ ] 최신 AI 리밸런싱 조언 조회(`/api/v1/portfolios/{id}/advice/latest`) API 응답이 JSON 구조화되어 프론트엔드에 표시되는지 검증.
    *   [ ] OpenAI API 지연 및 실패(Retry 3회 초과) 시 프론트엔드 에러 핸들링 확인.

*   **백테스트 시뮬레이터 (Portfolio Analysis Component)**
    *   [ ] 거치식 및 적립식(DCA) 백테스트 요청(`/api/v1/portfolios/{id}/analysis/backtest`) 시뮬레이션 결과(수익률, MDD 등) 검증.
    *   [ ] 프론트엔드 차트 컴포넌트에 백테스트 결과가 정확히 매핑되는지 확인.

### 3.2. 데이터 정합성 검증

시스템 내 주요 컴포넌트 간 데이터 상태가 일관되게 유지되는지 확인합니다.

*   **트랜잭셔널 아웃박스 패턴 (Transactional Outbox Pattern)**
    *   [ ] 포트폴리오 분석 결과 저장 (DB 트랜잭션).
    *   [ ] **BEFORE_COMMIT:** Outbox 테이블에 `READY_TO_PUBLISH` 상태로 이벤트 저장 확인.
    *   [ ] **AFTER_COMMIT:** Kafka 메시지 발행 성공 후 Outbox 상태가 `PUBLISHED`로 변경되는지 확인.
    *   [ ] **장애 시뮬레이션:** Kafka 브로커 다운 시 메시지 발행 실패 시뮬레이션 -> Outbox 상태 `FAILED` 전환 확인.
    *   [ ] **스케줄러 복구:** `FAILED` 상태 메시지가 3분 주기 스케줄러에 의해 재처리되어 `PUBLISHED` 상태로 복구되는지 검증.

*   **배치 처리 및 일일 데이터 통합 (Spring Batch)**
    *   [ ] KIS API에서 EOD(End Of Day) 시세 수집 배치 스텝 실행 (`stockwellness-batch`).
    *   [ ] 수집된 EOD 데이터를 기반으로 기술 지표(RSI, MACD)가 정상적으로 계산되어 데이터베이스에 저장되는지 확인.
    *   [ ] 배치 수행 결과가 Kafka 이벤트로 발행되고 관련 컴포넌트(DB, 프론트엔드)에 최신 상태로 반영되는지 확인.
    *   [ ] 대량 데이터 처리 시 Virtual Threads 기반 병렬 처리 성능 확인.

*   **계층적 캐싱 전략 (Redis)**
    *   [ ] EOD 시세 및 공통 데이터 API 호출 시 DB 쿼리 대신 Redis 캐시 히트 발생 여부 검증 (응답 속도 및 로그 확인).
    *   [ ] 배치로 EOD 데이터가 갱신될 때 관련 Redis 캐시가 정상적으로 무효화(Eviction) 및 갱신되는지 확인.
    *   [ ] 세션 토큰 및 관심 종목 랭킹 데이터 등이 설정된 TTL 내에 정상적으로 유지되는지 확인.

### 3.3. 예외 및 에러 핸들링

*   [ ] API 표준 에러 응답 형식 일치 확인 (G*, A*, M*, P* 등 ErrorCode 매핑).
    ```json
    { "status": 400, "code": "G001", "message": "...", "timestamp": "...", "traceId": "...", "errors": [] }
    ```
*   [ ] 외부 API(KIS API, OpenAI) 호출 실패 및 타임아웃 발생 시 적절한 에러 코드가 프론트엔드로 전달되는지 확인.
*   [ ] 프론트엔드 500 에러 처리 및 사용자 친화적 오류 메시지 표시 확인.

## 4. 진행 절차

1.  **환경 구성:** 로컬 인프라(Docker Compose) 기동 및 `api`, `batch` 서버 실행.
2.  **테스트 스크립트 작성:** 핵심 API에 대한 Postman/cURL 테스트 스크립트 준비.
3.  **컴포넌트 별 단위/통합 테스트:** Swagger UI를 통한 API 단독 테스트 진행.
4.  **프론트엔드 연동 테스트:** 로컬 `stockwellness-front` 기동 후 화면 흐름에 따른 시나리오 테스트.
5.  **이벤트 모니터링:** Outbox DB 테이블, Kafka Console Consumer, Redis CLI를 활용한 상태 관찰.
6.  **결과 기록 및 결함 추적:** 발견된 이슈를 Jira/GitHub Issues 등에 등록하여 관리.
