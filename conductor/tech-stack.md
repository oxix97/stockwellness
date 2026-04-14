# Tech Stack

## Language

- **Java 21**: Virtual Threads 활용 → EOD 데이터 수집·외부 API 호출 성능 극대화

## Frameworks

- **Spring Boot 3.4.1**: REST API 메인 프레임워크
- **Spring Batch**: EOD 금융 데이터 수집·정산 배치
- **Spring Security**: OAuth2 소셜 로그인 + JWT 인증/인가
- **Spring AI**: OpenAI 연동 추상화

## Database & Persistence

- **PostgreSQL**: 포트폴리오·사용자 등 트랜잭션 정합성이 필요한 메인 RDB
- **Redis**: API 응답 캐싱, JWT Refresh Token 관리
- **JPA + QueryDSL**: 객체 중심 데이터 접근 + 타입 안정성 보장 동적 쿼리
- **Flyway**: DB 마이그레이션 버전 관리

## Infrastructure & Messaging

- **Apache Kafka**: Transactional Outbox 패턴 기반 비동기 이벤트 전달 (배치 결과 발행 등)
- **Docker Compose**: 로컬 개발 인프라 (PG, Redis, Kafka)

## AI & Third-Party

- **OpenAI GPT-4o-mini**: 포트폴리오 분석 및 AI 리밸런싱 어드바이저 생성
- **KIS (한국투자증권) API*_*_: 주가·섹터 데이터 수집
- **Slack Webhook**: 배치 실패 실시간 모니터링 알림

## Resilience

- Resilience4j: Circuit Breaker / Retry (외부 API 호출 안정성)

## Build & Docs

- **Gradle Kotlin DSL**: 멀티 모듈 의존성·빌드 라이프사이클 관리
- **Spring REST Docs + OpenAPI 3**: 테스트 기반 API 문서 자동화
- **P6Spy**: SQL 쿼리 로깅 (개발 환경)
