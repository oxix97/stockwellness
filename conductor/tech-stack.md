# Technology Stack

## 1. Core Platform
- **Language:** Java 21 (leveraging Virtual Threads for high-concurrency portfolio simulations)
- **Framework:** Spring Boot 3.4.1
- **Architecture:** Pragmatic Hexagonal Architecture (Domain/JPA Hybrid Model for efficiency)

## 2. Data & Persistence
- **Primary Database:** PostgreSQL
- **Caching:** Redis (for session management and technical indicator caching)
- **Query Engine:** QueryDSL (for type-safe dynamic queries)
- **In-Memory Database:** H2 (for integration and repository testing)

## 3. Specialized Engines
- **Batch Processing:** Spring Batch (daily data pipelines for KRX and market history)
- **Quantitative Analysis:** ta4j (technical indicator calculation: RSI, MACD, etc.)
- **AI Integration:** Spring AI (OpenAI integration for report generation and insights)

## 4. Security & Integration
- **Security:** Spring Security & JWT (token-based authentication)
- **API Documentation:** Spring REST Docs (test-driven) & Swagger/OpenAPI 3 (interactive UI)
- **Observability:** P6Spy (SQL query monitoring)

## 5. Infrastructure & Devops
- **Containerization:** Docker & Docker Compose (local development environment)
- **CI/CD:** GitHub Actions (Automated build, test, and containerization)
- **Registry:** GitHub Container Registry (GHCR)
- **Messaging:** Kafka (event-driven architecture for data consistency)
- **Deployment Target:** Kubernetes (AWS EKS) for high availability and scalability
