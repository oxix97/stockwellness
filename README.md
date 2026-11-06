<h1 align="center">$\bf{\large{\color{#2EBE7A} Stockwellness \ Backend \ Server }}$</h1>

> 개인 투자자를 위한 자산 배분 시뮬레이터 및 AI 기반 투자 분석 서비스.
> 감정적 거래를 데이터 기반 의사결정으로 대체하는 것이 핵심 가치입니다.

## 개발 환경

성능 측정 가이드는 [k6/README.md](./k6/README.md)를 참고합니다.
백엔드 개발 작업 가이드는 [backend-development-guide.md](documents/backend-development-guide.md)를 참고합니다.

### Language
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)

### Framework
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)

### Database
![PostgreSQL](https://img.shields.io/badge/postgresql-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)

### Infra & Messaging
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-000?style=for-the-badge&logo=apachekafka)
![Nginx](https://img.shields.io/badge/nginx-%23009639.svg?style=for-the-badge&logo=nginx&logoColor=white)
![n8n](https://img.shields.io/badge/n8n-EA4B71?style=for-the-badge&logo=n8n&logoColor=white)

### AI
![OpenAI](https://img.shields.io/badge/OpenAI-74aa9c?style=for-the-badge&logo=openai&logoColor=white)

### DevOps & CI/CD
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

<hr>

## Key Dependencies and Features

### 1. Pragmatic Hexagonal Architecture + DDD
- 멀티 모듈 구조로 도메인 책임을 `stockwellness-core`, `stockwellness-api`, `stockwellness-batch`로 분리
- `core`가 모든 도메인 엔티티와 비즈니스 로직을 소유하고, `api`·`batch`는 얇은 어댑터 역할만 수행
- Ports & Adapters 패턴으로 외부 의존성(DB, Redis, KIS API, OpenAI)을 도메인으로부터 격리

### 2. Transactional Outbox Pattern
- `PortfolioStatBatchService`가 포트폴리오 통계와 `PENDING` Outbox 행을 같은 PostgreSQL 트랜잭션에 저장
- Batch의 릴레이가 5초 간격으로 `PENDING` 행을 조회해 Kafka 발행을 요청
- 발행 요청과 DB 저장은 하나의 원자적 트랜잭션이 아니며, 현재 비동기 발행 결과는 로그에만 남음

### 3. Spring Batch 기반 EOD 데이터 파이프라인
- 매 영업일 장 마감 후 한국투자증권 KIS API에서 일일 시세(EOD) 수집
- RSI, MACD 등 기술 지표를 배치 단계에서 사전 계산하여 저장 (ta4j 활용)
- Virtual Threads 기반 병렬 처리로 대량 종목 데이터 수집 속도 최적화

### 4. PortfolioFacade 기반 통합 오케스트레이션
- 포트폴리오 백테스트, 건강 진단(MDD·Sharpe·Beta), 리밸런싱, AI 어드바이저를 단일 Facade로 통합
- 거치식/적립식(DCA) 전략 모두 지원하는 백테스트 시뮬레이터
- 자산군·업종·국가별 분산 분석 및 종목 간 상관관계 행렬 제공

### 5. AI 리밸런싱 어드바이저
- OpenAI GPT-4o-mini와 Spring AI 연동으로 포트폴리오 분석 결과 기반 투자 조언 생성
- BeanOutputConverter로 JSON 구조화 응답, 재시도 3회 및 Temperature 0.3으로 일관성 보장

### 6. 계층적 캐싱 전략
- Redis를 활용한 EOD 시세, 세션, JWT 다단계 캐싱
- `@Cacheable` + RedisTemplate 조합으로 외부 API 호출 최소화

### 7. Blue-Green API 배포
- `deploy-api.sh`는 대기 슬롯을 시작하고 헬스체크한 뒤 Nginx upstream을 전환하며, 안정성 확인 실패 시 되돌리는 경로를 둠
- Batch는 별도 배포 스크립트로 갱신하며 API Blue-Green 슬롯을 사용하지 않음
- 워크플로우와 스크립트 설정은 아래 배포 파이프라인 그림에 정리함

<hr>

## 아키텍처

### 소프트웨어 모듈

`stockwellness-api`와 `stockwellness-batch`는 `stockwellness-core`에 의존합니다. 도메인, 애플리케이션 서비스, 포트와 영속성 어댑터는 `core`가 소유합니다.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/diagrams/software-modules-dark.png">
  <img src="docs/diagrams/software-modules.png" alt="stockwellness-api와 stockwellness-batch가 stockwellness-core의 입력 포트를 통해 의존하는 멀티 모듈 구조">
</picture>

### 런타임 구성

아래 그림은 저장소의 프로덕션 Compose 및 Nginx 설정을 요약합니다. Blue/Green 두 API 슬롯은 설정에 선언되어 있으며, 어느 슬롯이 현재 운영 중인지는 이 그림이 나타내지 않습니다.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/diagrams/runtime-topology-dark.png">
  <img src="docs/diagrams/runtime-topology.png" alt="Nginx가 Blue 또는 Green API 슬롯으로 요청을 전달하고 API와 Batch가 PostgreSQL, Redis, Kafka를 공유하는 런타임 구성">
</picture>

### 배포 파이프라인

API와 Batch 배포 워크플로우는 태그 push 후 동일 SHA의 CI 성공을 확인하고, 이미지를 GHCR에 올린 다음 Tailscale을 거쳐 n8n webhook을 호출합니다. 그림은 저장소의 workflow와 배포 스크립트 설정을 나타내며 운영 서버의 배포 성공을 증명하지 않습니다.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/diagrams/deployment-pipeline-dark.png">
  <img src="docs/diagrams/deployment-pipeline.png" alt="태그 push에서 CI 확인, GHCR 이미지 빌드, Tailscale과 n8n webhook을 거쳐 API 또는 Batch 배포 스크립트로 이어지는 파이프라인">
</picture>

### Transactional Outbox

`PortfolioStatBatchService.saveStatsAndEvent()`는 포트폴리오 통계와 `PENDING` Outbox 행을 같은 DB 트랜잭션에서 저장합니다. 이 트랜잭션은 DB 내부의 두 저장을 함께 커밋하거나 롤백하지만 Kafka 발행까지 묶지는 않습니다.

```java
// PortfolioStatBatchService.saveStatsAndEvent() 일부
outboxPort.save(OutboxEvent.create(
        KafkaTopicConfig.PORTFOLIO_ANALYSIS_COMPLETED_TOPIC, payload));
```

Batch의 `OutboxRelay`는 5초 간격으로 `PENDING` 행을 읽습니다. Kafka 어댑터는 `KafkaTemplate.send()`의 Future 결과를 기다리지 않고 콜백에서 성공·실패 로그만 남깁니다. 어댑터 호출이 정상 반환하면 릴레이는 곧바로 상태를 `COMPLETED`로 바꾸고, 동기 예외를 받으면 `FAILED`로 바꿉니다. 다음 조회는 `PENDING`만 찾으므로 `FAILED` 재처리는 구현돼 있지 않습니다. 비동기 발행 실패가 발생해도 이미 `COMPLETED`가 저장될 수 있습니다.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/diagrams/outbox-state-dark.png">
  <img src="docs/diagrams/outbox-state.png" alt="Outbox 상태 흐름: PENDING은 send 호출의 정상 반환 뒤 COMPLETED가 되고 동기 예외 뒤 FAILED가 되며 Future 콜백은 상태를 바꾸지 않음">
</picture>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/diagrams/outbox-sequence-dark.png">
  <img src="docs/diagrams/outbox-sequence.png" alt="통계와 Outbox를 DB 트랜잭션으로 저장한 뒤 별도 릴레이 트랜잭션이 PENDING을 조회하고 Kafka send 호출 후 Outbox 상태를 커밋하는 시퀀스">
</picture>

`portfolio-analysis-completed`를 구독하는 `@KafkaListener`는 저장소 코드에서 확인되지 않았습니다. 다른 토픽의 소비 동작은 별도입니다.

| 토픽 | 소비자 | 코드와 테스트에서 확인한 동작 |
|---|---|---|
| `market-score-calculated` | `WeatherEventConsumer` | 처리 실패 시 최초 처리를 포함해 3회 시도하고, 최종 실패 메시지를 `-dlt`로 보냅니다. 통합 테스트는 3회 호출과 DLT의 원래 key/value를 확인합니다. DLT 핸들러는 로그만 기록합니다. |
| `stock-price-updated` | `StockPriceUpdateConsumer` | 캐시 무효화와 후속 분석 트리거를 호출합니다. 예외를 내부에서 잡아 기록하므로 해당 예외가 Kafka listener 밖으로 전파되지 않습니다. |

<hr>

## Component & API URI Collection

### Auth Component

소셜 로그인(OAuth2) 기반 인증 및 JWT 토큰 관리를 담당하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/auth/login` | POST | 소셜 로그인 (Kakao · Google OAuth2) |
| `/api/v1/auth/reissue` | POST | Access Token 재발급 |
| `/api/v1/auth/logout` | POST | 로그아웃 (Refresh Token 무효화) |

<br>

### Member Component

회원 정보 조회·수정·탈퇴 및 알림 설정을 관리하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/members/me` | GET | 내 정보 조회 |
| `/api/v1/members/me` | PUT | 내 정보 수정 |
| `/api/v1/members/me` | DELETE | 회원 탈퇴 |
| `/api/v1/members/me/notifications` | GET | 알림 설정 조회 |
| `/api/v1/members/me/notifications` | PUT | 알림 설정 변경 |

<br>

### Portfolio Component

포트폴리오 CRUD 및 건강 진단·AI 리밸런싱 어드바이저를 담당하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/portfolios` | POST | 포트폴리오 생성 |
| `/api/v1/portfolios` | GET | 내 포트폴리오 목록 조회 |
| `/api/v1/portfolios/{portfolioId}` | GET | 포트폴리오 상세 조회 |
| `/api/v1/portfolios/{portfolioId}` | PUT | 포트폴리오 수정 (구성 종목 변경) |
| `/api/v1/portfolios/{portfolioId}` | DELETE | 포트폴리오 삭제 |
| `/api/v1/portfolios/{portfolioId}/health` | GET | 포트폴리오 건강 진단 (MDD · Sharpe · Beta) |
| `/api/v1/portfolios/{portfolioId}/advice/latest` | GET | 최신 AI 리밸런싱 조언 조회 |

<br>

### Portfolio Analysis Component

백테스트, 성과 분석, 비중 분석, 리밸런싱 가이드를 담당하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/portfolios/{portfolioId}/analysis/valuation` | GET | 성과 분석 (평가 가치 및 수익률) |
| `/api/v1/portfolios/{portfolioId}/analysis/diversification` | GET | 비중 분석 (자산군 · 업종 · 국가) |
| `/api/v1/portfolios/{portfolioId}/analysis/rebalancing` | GET | 리밸런싱 가이드 조회 |
| `/api/v1/portfolios/{portfolioId}/analysis/summary` | GET | 분석 요약 정보 조회 |
| `/api/v1/portfolios/{portfolioId}/analysis/backtest` | POST | 백테스트 시뮬레이션 (거치식 · DCA) |
| `/api/v1/portfolios/{portfolioId}/analysis/correlation` | GET | 종목 간 상관관계 행렬 조회 |

<br>

### Stock Component

종목 탐색, 기본 정보, 시세 차트, 수익률 데이터를 제공하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/stocks/search` | GET | 통합 종목 검색 (키워드 · 시장 · 상태 필터) |
| `/api/v1/stocks/search/history` | GET | 최근 검색어 조회 |
| `/api/v1/stocks/search/history` | DELETE | 최근 검색어 개별 삭제 |
| `/api/v1/stocks/search/history/all` | DELETE | 최근 검색어 전체 삭제 |
| `/api/v1/stocks/popular-search` | GET | 인기 검색어 Top 10 조회 |
| `/api/v1/stocks/new-listings` | GET | 신규 상장 종목 조회 |
| `/api/v1/stocks/{ticker}` | GET | 종목 상세 정보 조회 |
| `/api/v1/stocks/{ticker}/prices/history` | GET | 차트용 과거 가격 데이터 조회 (기간 · 주기 선택) |
| `/api/v1/stocks/{ticker}/returns` | GET | 기간별 수익률 및 벤치마크 대비 수익률 조회 |

<br>

### Sector Component

섹터 랭킹, 수급 분석, 섹터 상세 및 시장 비교를 제공하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/sectors/ranking/fluctuation` | GET | 등락률 상위 섹터 조회 |
| `/api/v1/sectors/ranking/supply` | GET | 수급 상위 섹터 조회 |
| `/api/v1/sectors/{sectorCode}/detail` | GET | 섹터 상세 및 진단 정보 조회 |
| `/api/v1/sectors/{sectorCode}/comparison` | GET | 섹터 vs 시장 비교 분석 조회 |

<br>

### Market Component

시장 지수 데이터를 제공하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/market/indexes` | GET | 시장 지수 조회 (KOSPI · KOSDAQ · S&P500) |

<br>

### Watchlist Component

관심 종목 그룹 및 종목 항목을 관리하는 컴포넌트

| URI | Method | 설명 |
|---|---|---|
| `/api/v1/watchlist/groups` | POST | 관심 종목 그룹 생성 |
| `/api/v1/watchlist/groups` | GET | 내 관심 종목 그룹 목록 조회 |
| `/api/v1/watchlist/groups/{groupId}` | PATCH | 그룹명 수정 |
| `/api/v1/watchlist/groups/{groupId}` | DELETE | 그룹 삭제 |
| `/api/v1/watchlist/groups/{groupId}/items` | POST | 그룹에 종목 추가 |
| `/api/v1/watchlist/groups/{groupId}/items` | GET | 그룹 내 종목 목록 조회 |
| `/api/v1/watchlist/groups/{groupId}/items/{ticker}` | DELETE | 그룹에서 종목 제거 |
| `/api/v1/watchlist/groups/{groupId}/items/{ticker}/note` | PATCH | 종목 메모 수정 |
