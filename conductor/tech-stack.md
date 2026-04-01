# Tech Stack: Stockwellness Backend Server

## 1. Programming Language
*   **Java 21:** 최신 LTS 버전으로, 가상 스레드(Virtual Threads) 등 최신 기능을 활용하여 동시성 처리 성능을 극대화합니다.

## 2. Frameworks & Libraries
*   **Spring Boot 3.4.1:** 애플리케이션의 핵심 프레임워크로, 웹 API 및 의존성 관리를 담당합니다.
*   **Spring Batch 6:** 대용량 금융 데이터 수집 및 지표 계산을 위한 배치 파이프라인 처리에 사용됩니다.
*   **Spring Security 6:** 애플리케이션 보안, 인증 및 인가 처리를 담당합니다.
*   **Spring Data JPA & QueryDSL:** 타입 세이프한 동적 쿼리 작성과 데이터베이스 영속성을 관리합니다.
*   **Spring AI:** OpenAI (GPT 모델) 연동을 통해 AI 어드바이저 기능을 구현합니다.

## 3. Database & Cache
*   **PostgreSQL:** 주요 트랜잭션 데이터(회원, 포트폴리오, 시세 데이터 등)를 저장하는 강력한 관계형 데이터베이스입니다.
*   **Redis:** EOD 시세 캐싱, 세션 관리, JWT 토큰 저장 등 빠른 읽기 응답을 위한 인메모리 데이터 스토어입니다.

## 4. Message Broker
*   **Apache Kafka:** 비동기 이벤트 처리(예: 포트폴리오 분석 완료 이벤트) 및 Transactional Outbox 패턴 구현에 사용되어 시스템 간 높은 결합도를 낮추고 데이터 정합성을 보장합니다.

## 5. Security & Authentication
*   **JWT (JSON Web Token):** 무상태(Stateless) 기반의 빠르고 안전한 API 인증 토큰으로 사용됩니다.
*   **OAuth2:** 소셜 로그인(Kakao, Google)을 지원하여 사용자 편의성과 보안성을 강화합니다.

## 6. Infrastructure & DevOps
*   **Nginx:** 리버스 프록시 및 Blue-Green 무중단 배포를 위한 로드 밸런서 역할을 수행합니다.
*   **Docker:** 애플리케이션과 인프라 요소(DB, Redis, Kafka)의 컨테이너화 및 환경 일관성을 보장합니다.
*   **GitHub Actions:** 소스 코드 빌드, 테스트, 도커 이미지 생성 등 CI/CD 파이프라인의 핵심 도구입니다.
*   **n8n:** GitHub Actions의 웹훅을 받아 운영 서버의 배포 스크립트를 실행하고 Slack 알림을 전송하는 워크플로우 자동화 도구입니다.