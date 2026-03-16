# StockWellness Project Rules

이 파일은 StockWellness 프로젝트의 개발 표준과 Gemini 에이전트의 행동 지침을 정의합니다.

## 1. 프로젝트 개요
사용자의 주식 포트폴리오 건강 상태를 진단하고 AI 기반 조언을 제공하는 시스템입니다. 확장성과 유지보수성을 위해 멀티 모듈 및 프래그마틱 헥사고날 아키텍처를 지향합니다.

## 2. 기술 스택 (Tech Stack)
- **Language**: Java 21 (Virtual Threads 활용)
- **Framework**: Spring Boot 3.4.1
- **Build Tool**: Gradle (Kotlin DSL)
- **Modules**: Multi-module 구조
  - `stockwellness-core`: 도메인 엔티티, 비즈니스 로직(UseCase), 영속성 계층 (JPA/QueryDSL/Redis)
  - `stockwellness-api`: REST API 어댑터, OAuth2 인증 (Kakao), JWT
  - `stockwellness-batch`: 데이터 전처리 및 대량 수집 (Spring Batch)
- **Persistence**: PostgreSQL, Redis, H2 (Test)
- **Messaging**: Apache Kafka (Event-driven)
- **AI**: Spring AI (OpenAI)
- **Documentation**: Spring REST Docs, Swagger

## 3. 에이전트 행동 지침 (Agent Behavior)
- **언어 설정**: 모든 답변과 제안은 **한국어(한글)**로 작성한다.
- **문서 참조**: 작업 진행 시 `conductor/` 디렉토리의 가이드(`API-standard.md`, `product-guidelines.md` 등)를 최우선으로 참고한다.
- **아키텍처**: 모든 비즈니스 로직은 `stockwellness-core` 모듈을 중심으로 작성하며, 모듈 간 의존성 규칙을 엄격히 준수한다.
- **브레인스토밍**: 요청받은 기능의 엣지 케이스를 확인하고 설계적 의문을 먼저 제기한다.
- **계획 수립** : 수정할 파일 목록과 구현 로직의 초안을 담은 [Plan]을 제시하고 승인받는다.
- **자가 검토** : 코드 작성 후 GEMINI.md의 규칙(Import, 아키텍처 등)을 준수했는지 스스로 비판하고 수정한다.

## 4. GitHub 이슈 및 Git 브랜칭 전략
- 사용자 정의 규칙에 따라 다음 프로세스를 엄격히 준수한다.

### 4.1. GitHub 이슈 구조
1. **상위 이슈 (Parent Issue)**: 전체 기능이나 목표를 대표하는 1개의 이슈를 생성한다.
2. **하위 이슈 (Sub-issues)**: 세부 기술 작업 단위로 N개의 이슈를 생성한다.
3. **연동**: 생성된 모든 하위 이슈를 상위 이슈에 링크(Sub-issue 연결)하여 계층 구조로 관리한다.

### 4.2. Git 브랜칭 명명 규칙
- **하위 이슈 브런치**: `task/#[하위 이슈 번호]-[작업 내용]`
- **상위 이슈 브런치**: `[유형]/#[상위 이슈 번호]-[작업 내용]`
  - 유형 예시: `feature`, `refactor`, `chore`, `test`, `fix` 등

### 4.3. 작업 워크플로우
1. 하위 브런치(`task/*`)에서 작업 후 상위 브런치(`[유형]/*`)로 병합한다.
2. 전체 작업이 마무리되면 상위 브런치에서 `develop` 브런치로 **Pull Request**를 생성한다.

## 5. Gemini Added Memories

- GitHub 이슈 생성 시 항상 상위 이슈 1개와 하위 이슈 N개의 계층 구조를 적용한다.
- Git 브랜칭 전략 규칙(하위 task/*, 상위 feature/* 등)을 준수한다.
- **Gemini 3.1 Pro**: 아키텍처 설계, 퀀트 알고리즘, 복잡한 문제 해결.
- **Gemini 3 Flash**: API 보일러플레이트, 단위 테스트, 문서화, 단순 리팩토링.

## 6. 개발 원칙 및 클린 코드 가이드라인 (Coding Standards)

### 6.1. Java 21 모던 문법 및 가독성
- **Import 우선 정책**: 모든 외부 클래스는 파일 상단에 `import`하여 사용한다. 코드 내에서 패키지 경로를 직접 노출하는 **FQCN(Fully Qualified Class Name) 사용을 엄격히 금지**한다. (예: `java.util.List` 대신 `List` 사용)
- **불변 객체 지향**: DTO, Command, Event 객체는 Java 21의 `record`를 사용하여 데이터 불변성을 보장한다.
- **도메인 모델 설계**: 상태 변경 로직은 엔티티 내부에 응집시키며, `sealed class/interface`를 활용하여 도메인 상태를 명확히 정의한다.
- **가상 스레드(Virtual Threads)**: I/O 집약적인 작업(외부 API 호출, Batch 작업)은 `VirtualThreadPerTaskExecutor`를 활용하여 성능을 최적화한다.

### 6.2. 헥사고날 아키텍처 및 DDD 준수
- **의존성 방향**: 모든 의존성은 외부에서 `stockwellness-core`(도메인)로 향해야 한다. 도메인 계층은 외부 라이브러리나 기술 스택(JPA, Kafka 등)에 의존하지 않는다.
- **Port & Adapter 분리**:
  - 비즈니스 요구사항은 `Port`(Interface)로 정의한다.
  - 외부 연동(DB, API, Messaging)은 `Adapter`에서 구현하며, 도메인 로직과 철저히 격리한다.
- **Service의 역할**: 서비스는 여러 도메인 모델의 협력을 조율(Orchestration)하는 역할만 수행하며, 비즈니스 로직을 직접 구현하는 'Fat Service'를 지양한다.

### 6.3. 보안 및 데이터 정책
- **No Password**: 자체 비밀번호 저장을 금지하며, 모든 인증은 OAuth2(Social)로 처리한다. `Member` 엔티티에 `password` 필드 생성을 금지한다.
- **EOD 데이터 원칙**: 실시간 데이터 대신 시장 마감 후의 종가(End of Day) 데이터만 취급하며, 기술적 지표(RSI, MACD 등)는 배치 단계에서 미리 계산하여 저장한다.

### 6.4. 메시징 및 트랜잭션
- **Transactional Outbox**: DB 저장과 Kafka 이벤트 발행의 원자성을 보장하기 위해 Outbox 패턴을 적용한다.
- **Exactly-once**: Kafka 메시지 발행 시 데이터 중복 방지 및 정확한 전달을 보장하는 설정을 지향한다.

## 7. 에러 처리 및 로깅
- **비즈니스 예외**: `RuntimeException`을 상속받은 커스텀 예외를 사용하며, API 계층의 `GlobalExceptionHandler`에서 응답을 규격화한다.
- **로깅**: SLF4J를 사용하며, 중요한 비즈니스 흐름과 에러 발생 지점에는 반드시 추적 가능한 로그를 남긴다.

## 8. AI 모델 활용 전략 (AI Model Utilization)

### 8.1. Gemini 3.1 Pro: 전략적 의사결정 및 복잡한 설계
- **활용 범위**:
  - 헥사고날 아키텍처의 포트/어댑터 설계 및 도메인 모델링.
  - 퀀트 알고리즘(수익률 계산 로직, 기술적 지표 조합 전략) 수립.
  - 보안 취약점 점검 및 코드 리뷰.
  - 복잡한 SQL 쿼리 및 Kafka 파티셔닝 전략 수립.
- **행동 지침**: 고도의 논리적 추론이 필요한 작업에서 "Deep Reasoning" 모드를 활성화하여 발생 가능한 부작용(Side-effect)을 선제적으로 보고한다.

### 8.2. Gemini 3 Flash: 속도 및 효율성 중심 작업
- **활용 범위**:
  - CRUD API 기초 코드 생성 및 단위 테스트 코드 작성.
  - 배치를 통해 수집된 데이터의 요약 및 기술 문서 초안 작성.
  - 단순 오타 수정 및 스타일 가이드 준수 여부 확인.
  - 대량의 API 응답 JSON 스키마 파싱 보조.
- **행동 지침**: 빠른 응답이 필요한 반복 작업에 최적화하며, 토큰 사용 효율을 극대화하여 비용 최적화된 코드를 제안한다.

### 8.3. 모델 전환 기준 (Model Switching)
- 아키텍처의 큰 틀이 변하거나 데이터 무결성이 중요한 로직을 다룰 때는 **Gemini 3.1 Pro**를 우선 참조한다.
- 인프라 설정(Docker, K8s manifest) 및 반복적인 Boilerplate 코드 작성 시에는 **Gemini 3 Flash**의 민첩성을 활용한다.