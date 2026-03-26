# 🛠️ StockWellness Tech Stack

이 문서는 StockWellness 시스템을 구동하는 핵심 기술 스택을 정의합니다.

## Language
- **Java 21**: 최신 LTS 버전으로, Virtual Threads를 적극 활용하여 EOD 데이터 수집 및 외부 API 호출 시의 성능을 극대화합니다.

## Frameworks
- **Spring Boot 3.4.1**: 코어 애플리케이션 및 REST API 개발을 위한 메인 웹 프레임워크.
- **Spring Batch**: 대량의 금융 데이터 정기 수집(EOD) 및 정산 작업을 위한 배치 프레임워크.
- **Spring Security**: OAuth2 소셜 로그인 및 JWT 기반의 안전한 인증/인가 처리.

## Database & Persistence
- **PostgreSQL**: 포트폴리오, 사용자 정보 등 강력한 트랜잭션과 정합성이 요구되는 메인 관계형 데이터베이스.
- **Redis**: API 응답 캐싱, 사용자 세션, JWT 관리를 위한 In-memory 데이터 스토어.
- **JPA & QueryDSL**: 객체 중심의 데이터 접근과 타입 안정성이 보장된 복잡한 동적 쿼리 작성.

## Infrastructure & Messaging
- **Apache Kafka**: Transactional Outbox 패턴과 결합하여 배치 작업 결과 발행 등 분산 시스템 환경에서 안정적인 비동기 이벤트 전달 보장.

## AI & Third-Party
- **OpenAI (GPT-4o-mini)**: Spring AI와 연동하여 사용자 포트폴리오 데이터를 분석하고 지능형 투자 조언(Rebalancing Advisor)을 생성.
- **Slack Webhook**: 배치 작업 실패 시 실시간 모니터링 알림을 위해 연동.

## Build Tool
- **Gradle (Kotlin DSL)**: 멀티 모듈 프로젝트의 의존성 및 빌드 라이프사이클 관리.