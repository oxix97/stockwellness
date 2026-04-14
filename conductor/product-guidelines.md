# product-guidelines.md (stockwellness)

## 1. 아키텍처 가이드라인 (Architectural Guidelines)

### 헥사고날 아키텍처 (Hexagonal Architecture)
프로젝트는 외부 환경의 변화에 유연하게 대응하기 위해 헥사고날(Ports and Adapters) 구조를 엄격히 준수한다.

-   **Domain Layer (`core` 모듈):** 외부 라이브러리 의존성(Spring 포함)을 최소화한 순수 Java 객체로 구성한다. 비즈니스 규칙과 핵심 로직은 반드시 이곳에 위치한다.
-   **Application Layer (`core` 모듈):** UseCase 인터페이스와 서비스를 정의한다. 외부 세계와의 소통 창구인 **Port**(Inbound/Outbound)를 정의하여 인프라와의 결합도를 낮춘다.
-   **Adapter Layer (`api`, `batch` 모듈):**
    -   **Inbound Adapter:** Web Controller, Kafka Consumer, Scheduler.
    -   **Outbound Adapter:** Persistence(JPA/QueryDSL), External API Client(KIS, OpenAI), Message Publisher(Kafka).

### 도메인 주도 설계 (DDD)
-   **Bounded Context:** 서비스 규모에 따라 패키지를 명확히 분리하며, 컨텍스트 간 직접적인 엔티티 참조 대신 ID 기반 참조 또는 이벤트를 통한 통신을 권장한다.
-   **Aggregate:** 데이터 변경의 단위인 Aggregate Root를 정의하고, 한 트랜잭션 내에서는 하나의 Aggregate만 수정하는 것을 원칙으로 한다.

---

## 2. Java 21 & Spring Boot 3.3 표준 (Coding Standards)

### 모던 Java 문법 활용
-   **Record:** DTO, Command, Result 객체는 반드시 `record`를 사용하여 불변성을 보장한다.
-   **Sealed Classes & Interface:** 고정된 도메인 타입 계층(예: `CashFlowModel`, `SignalType`)은 `sealed` 키워드를 사용하여 패턴 매칭의 이점을 활용한다.
-   **Switch Pattern Matching:** 복잡한 조건문 대신 `switch` 패턴 매칭을 사용하여 가독성을 높인다.
-   **Virtual Threads:** I/O 병목이 발생하는 외부 API 호출 및 배치 작업 시 가상 스레드를 적극 활용한다 (`ApiAsyncConfig`, `BatchAsyncConfig` 참조).

### 생성자 주입
-   모든 스프링 빈은 생성자 주입을 사용한다. `Lombok`의 `@RequiredArgsConstructor`를 활용하되, 필수 의존성이 명확히 드러나도록 한다.

---

## 3. 데이터 전략 (Data & Persistence)

### EOD(End of Day) 중심 정책
-   **No Real-time:** 장중 실시간 데이터는 수집하지 않으며, 시장 마감 후의 종가 데이터만 처리한다.
-   **Separate Markets:** 한국(KR)과 미국(US) 시장의 배치 파이프라인은 시차에 따라 독립적으로 설계한다.

### AI 비용 최적화 (Pre-calculation)
-   AI 모델(OpenAI 등)에게 Raw Data를 전달하는 것을 금지한다.
-   배치 단계에서 **기술적 지표(RSI, MACD, 이동평균선 등)**를 미리 계산하여 DB에 저장하고, AI에게는 가공된 요약 정보(Insight Context)만 전달한다.

### 데이터 정합성 (Transaction & Messaging)
-   **Outbox Pattern:** DB 상태 변경과 Kafka 이벤트 발행의 원자성을 보장하기 위해 `outbox` 테이블을 거치는 Relay 방식을 사용한다.
-   **Idempotency:** 모든 배치 작업과 이벤트 리스너는 재실행 시 부작용이 없도록 멱등성을 보장해야 한다.

---

## 4. 보안 및 인증 (Security & Auth)

### OAuth2 Only
-   **No Password Column:** `Member` 테이블에 `password` 컬럼 생성을 엄격히 금지한다.
-   **Social Auth:** Google, Kakao 등 신뢰할 수 있는 소셜 로그인만 허용하며, JWT를 통해 인증 상태를 관리한다.
-   **Token Management:** Redis를 사용하여 Refresh Token을 관리하고 보안 유효기간을 제어한다.

---

## 5. 메시징 및 이벤트 (Messaging & Kafka)

### Kafka 전략
-   **Topic Naming:** `{service-name}.{domain}.{event-type}` 형식을 따른다. (예: `stockwellness.market.data-updated`)
-   **Schema Registry:** 이벤트 스키마의 하위 호환성을 위해 Schema Registry 활용을 지향한다.
-   **Back-pressure:** 대량 데이터 처리 시 Consumer의 처리 능력을 고려하여 파티셔닝 및 컨슈머 그룹 설정을 조정한다.

---

## 6. 테스트 및 품질 관리 (Testing & Quality)

### 테스트 계층
-   **Unit Test:** 도메인 로직 및 유틸리티 검증 (Spring Context 없이 실행).
-   **Integration Test:** `IntegrationTestSupport`를 상속받아 Testcontainers(PostgreSQL, Redis, Kafka) 환경에서 실제 인프라 의존성을 포함하여 테스트한다.
-   **API Test:** `RestDocsSupport`를 상속받아 API 명세서(Swagger/OpenAPI) 자동 생성과 연동한다.

### 예외 처리
-   `GlobalException`을 상속받은 도메인별 커스텀 예외를 정의한다.
-   모든 에러 응답은 `ApiResponse` 형식을 따르며, 한국어 메시지를 포함해야 한다.

---

## 7. 인프라 및 배포 (Infra & DevOps)

### GitOps & CI/CD
-   **GitHub Actions:** 모듈별(`api`, `batch`) 배포 워크플로우를 분리하여 운영 효율성을 높인다.
-   **Docker:** 모든 애플리케이션은 Multi-stage build를 통해 최적화된 Docker 이미지로 관리한다.