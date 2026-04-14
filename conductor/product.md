# product.md (stockwellness)

## 1. 프로젝트 정체성 (Project Identity)
- **서비스명:** stockwellness
- **목표:** 100% 기준 자산 배분 포트폴리오 시뮬레이션 및 AI 기술 지표 기반 투자 가이드 서비스.
- **핵심 가치:** - **Data-Driven:** 가공된 기술적 지표를 통한 객관적 투자 판단 지원.
    - **Architectural Excellence:** DDD 및 헥사고날 아키텍처 기반의 확장 가능한 기업급 구조.
    - **Efficiency:** Java 21 가상 스레드 및 AI 비용 최적화 전략(Pre-calculation) 적용.

## 2. 페르소나 및 역할 (Personas)
본 프로젝트는 다음 네 가지 관점을 융합하여 개발 및 유지보수한다.
1. **Architect:** 멀티 모듈 기반 헥사고날 아키텍처(Api, Batch, Core)의 정체성 유지 및 도메인 로직 격리.
2. **Quant Developer:** RSI, MACD, 이동평균선 등 기술적 지표 산출 로직의 정확성 및 데이터 파이프라인 최적화.
3. **DevOps Engineer:** EKS 기반 배포, GitHub Actions를 이용한 CI/CD, GitOps 기반의 인프라 관리.
4. **Product Manager:** 한국인 사용자를 위한 UI/UX 가이드라인 준수 및 시장 데이터 동기화 정책 관리.

## 3. 핵심 기술 스택 (Core Tech Stack)
### Backend & Language
- **Java 21:** Record, Sealed Class, Virtual Thread(Executor 최적화) 적극 활용.
- **Spring Boot 3.4.1:** Spring Data JPA, Spring Batch 5.x, Spring Security (OAuth2).
- **Build:** Gradle Kotlin DSL 기반 멀티 모듈 구조 (`api`, `batch`, `core`).

### Persistence & Messaging
- **PostgreSQL:** 메인 관계형 데이터베이스 (Flyway를 통한 스키마 버전 관리).
- **Redis:** Refresh Token 관리, 인기 검색어, 시세 데이터 캐싱.
- **Apache Kafka:** `market-data-updated` 이벤트 파이프라인 및 Outbox 패턴을 통한 데이터 정합성 보장.

### Infrastructure & Deployment
- **Container:** Docker, Amazon EKS (Kubernetes).
- **CI/CD:** GitHub Actions (API/Batch 모듈별 배포 워크플로우 분리).
- **AWS:** S3, Cloudfront, EC2, IAM.

## 4. 확정된 아키텍처 및 정책 (Immutable Principles)
### 인증 (Authentication)
- **Only OAuth2:** 자체 회원가입 기능을 두지 않으며 Google, Kakao 소셜 로그인만 허용한다.
- **No Password:** `Member` 엔티티 및 DB 테이블에 `password` 컬럼 생성을 절대 금지한다.

### 데이터 및 배치 전략
- **EOD Data Only:** 장중 실시간 데이터 대신 시장 마감 후의 종가(End of Day) 데이터만 처리한다.
- **Separated Batch:** 한국(KR)과 미국(US) 시장의 배치 파이프라인을 시차에 맞춰 별도로 운영한다.
- **AI Pre-calculation:** AI API 호출 비용 절감을 위해 배치 단계에서 기술적 지표를 선계산하여 DB에 저장한다. AI에게는 가공된 요약 정보만 전달한다.

### 아키텍처 구조 (Hexagonal Architecture)
- **Domain:** 외부 의존성 없는 순수 도메인 모델(유연하게 현재 @Entity로 사용).
- **Application Port/Service:** 유즈케이스 정의 및 인터페이스(Port)를 통한 인프라 격리.
- **Adapter:** Web(Controller), Persistence(RepositoryImpl), External API(KIS, yfinance) 등 외부 연동 구현.

## 5. 주요 기능 범위 (Functional Scope)
1. **포트폴리오 엔진:** Lump-sum(거치식), DCA(적립식) 모델 기반 백테스트 및 리밸런싱 시뮬레이션.
2. **시장 지표 진단:** 코스피/코스닥 마스터 정보 동기화 및 시장 온도(Market Weather) 산출.
3. **섹터 분석:** 업종별 선행 주식(Leading Stocks) 및 섹터별 기술적 지표 분석 리포트.
4. **AI 어드바이저:** AI가 생성한 포트폴리오 건강 점수(Health Score) 및 종목별 투자 의견 제공.

## 6. 개발 가이드라인 (Development Rules)
- **언어:** 모든 코드 주석, 문서, 예외 메시지는 **한국어**로 작성한다.
- **동시성 모델:** 가상 스레드(`ApiAsyncConfig`, `BatchAsyncConfig`)를 활용하여 I/O 병목을 최소화한다.
- **테스트:** `IntegrationTestSupport`를 활용하여 인프라(DB, Redis, Kafka)가 포함된 통합 테스트를 수행한다.
- **데이터 정합성:** 배치 작업 시 `OutboxRelay`를 통해 Kafka 이벤트 발행의 원자성을 보장한다.

## 7. 현재 상태 요약 (Current Status)
- **인프라:** 멀티 모듈 환경 및 CI/CD 파이프라인 구축 완료.
- **핵심 기능:** OAuth2 인증, KIS API 연동, 기본적인 포트폴리오 백테스트 엔진 구현 완료.
- **진행 중:** 섹터 코드 통합 리팩토링 및 AI 기반 섹터 진단 기능 고도화.