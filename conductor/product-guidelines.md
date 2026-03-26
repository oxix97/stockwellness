# 🛠️ StockWellness Backend Product Guidelines

본 가이드라인은 StockWellness 백엔드 시스템 설계 및 구현 시 준수해야 할 핵심 원칙을 정의합니다.

## 1. API Design Philosophy
- **Strict RESTful Principles**: 명확한 리소스(URI) 중심의 설계를 지향합니다. 표준 HTTP Method(GET, POST, PUT, DELETE)와 Status Code를 엄격하게 준수하여 예측 가능한 API를 제공합니다.
- **Self-Descriptive API**: RestDocs와 OpenAPI(Swagger)를 통해 클라이언트(FE)가 별도의 문의 없이도 연동할 수 있는 수준의 문서화를 유지합니다.

## 2. Data & Performance Strategy
데이터의 성격에 따라 서로 다른 최적화 전략을 하이브리드 방식으로 적용합니다.
- **Data Access & Optimization (JPA & QueryDSL)**: 데이터베이스 조회 시 JPA의 N+1 문제가 발생하지 않도록 Fetch Join, Entity Graph 등을 적극 활용하여 세밀하게 검토합니다. 복잡한 동적 쿼리나 통계/집계 쿼리는 유지보수성과 타입 안정성을 위해 **QueryDSL**을 필수적으로 적용합니다.
- **Aggressive Caching (읽기 전용/정적 데이터)**: 일일 마감(EOD) 시세, 섹터 정보, 과거 수익률 데이터 등 변경 빈도가 낮은 데이터는 Redis를 활용해 적극적으로 캐싱하여 응답 지연을 최소화합니다.
- **Strong Data Consistency (트랜잭션 데이터)**: 사용자의 포트폴리오 생성/수정, 관심 종목 변경 등 핵심 도메인의 상태 변경은 RDBMS(PostgreSQL)의 트랜잭션을 통해 강력한 데이터 정합성을 우선적으로 보장합니다.
- **Event-Driven Architecture (장기 실행 작업)**: AI 어드바이저 분석, 대규모 백테스트 등 시간이 오래 소요되는 로직은 Kafka와 Transactional Outbox 패턴을 활용한 비동기/이벤트 기반 처리로 API 서버의 부하를 분산하고 응답성을 높입니다.

## 3. Error Handling & Observability
- **Strict GlobalException Policy**: 애플리케이션 내에서 `RuntimeException`이나 `Exception`과 같은 Raw Exception을 직접 던지는 것을 엄격히 금지합니다. 반드시 프로젝트에 정의된 커스텀 비즈니스 예외 클래스를 사용하며, `GlobalExceptionHandler`를 통해 표준화된 형태의 에러 규격으로 응답을 반환해야 합니다. 이를 통해 클라이언트 개발자에게 명확한 오류 원인을 제공합니다.
- **Traceability**: 모든 API 요청과 비동기 이벤트 처리 과정은 SLF4J 로깅을 통해 철저히 추적 가능해야 하며, 오류 발생 지점을 명확히 식별할 수 있어야 합니다.

## 4. Architectural Boundaries
- **Pragmatic Hexagonal Architecture**: 모든 도메인 로직은 외부 인프라(DB, 외부 API)와 격리되어 `core` 모듈에 응집되어야 합니다. 어댑터(`api`, `batch`)는 도메인 모델을 오염시키지 않고 순수하게 입력 변환과 출력 포매팅 역할만 수행합니다.