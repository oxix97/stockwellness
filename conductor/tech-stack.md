# Technology Stack

## 1. Core Platform
- **Language:** Java 21 (leveraging Virtual Threads for high-concurrency portfolio simulations)
- **Framework:** Spring Boot 3.4.1
- **Architecture:** Multi-Module Gradle structure implementing Pragmatic Hexagonal Architecture (Core, API, Batch)

## 2. Data & Persistence
- **Primary Database:** PostgreSQL
- **Caching:** Redis (for session management, technical indicators, and hierarchical price data caching)
- **Query Engine:** QueryDSL (for type-safe dynamic queries)
- **In-Memory Database:** H2 (for integration and repository testing)
- **Data Utilities:** QueryTypeUtil (custom utility for explicit SQL type casting to ensure PostgreSQL/H2 compatibility)

## 3. Specialized Engines
- **Batch Processing:** Spring Batch (leveraging JdbcBatchItemWriter for high-performance bulk data ingestion)
- **Price Aggregation:** Java 21 Stream API (for high-performance in-memory weekly/monthly aggregation)
- **Quantitative Analysis:** ta4j (technical indicator calculation: RSI, MACD, etc.)
- **Portfolio Analysis Engine:** In-memory valuation and simulation engine (Backtest, Rebalancing) supporting professional-grade risk metrics (MDD, Sharpe, Beta).
- **Design Patterns:** Facade Pattern (PortfolioFacade for unified orchestration), Domain Utility Pattern (FinanceCalculationUtil, PortfolioMapperUtil).
- **AI Integration:** Spring AI (OpenAI integration for report generation and insights)

## 4. Security & Integration
- **Security:** Spring Security & JWT (token-based authentication)
- **API Documentation:** Spring REST Docs (test-driven) & Swagger/OpenAPI 3 (interactive UI)
- **Observability:** P6Spy (SQL query monitoring) and **Spring AOP based structured JSON logging** for system-wide traceability.
- **Health & Monitoring:** Spring Boot Actuator (standard metrics and health probes)

## 5. Infrastructure & Devops
- **Containerization:** Docker & Docker Compose (local development environment), n8n 커스텀 이미지(배포 오케스트레이터)
- **CI/CD:** GitHub Actions (Automated build, test, and containerization)
- **Registry:** GitHub Container Registry (GHCR)
- **Messaging:** Apache Kafka & Spring Kafka (Event-driven architecture for decoupling Batch and API, ensuring data consistency via cache invalidation events)
- **Deployment Visibility:** Slack 실시간 상세 진단 리포팅 및 배포 이력 기록 로직 도입
- **Deployment Target:** Kubernetes (AWS EKS) for high availability and scalability
