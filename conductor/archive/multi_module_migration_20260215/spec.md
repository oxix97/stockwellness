# Specification: Multi-Module Migration (Core, API, Batch)

## 1. Overview
The Stockwellness project is currently a single-module Spring Boot application. To improve maintainability, scalability, and operational robustness, we are migrating to a multi-module Gradle project structure consisting of three primary modules: `stockwellness-core`, `stockwellness-api`, and `stockwellness-batch`.

## 2. Motivations & Goals
- **Architectural Clarity:** Enforce strict boundaries between the Domain/Infrastructure (`core`), the Web Adapter (`api`), and the Batch Adapter (`batch`).
- **Resource Profile Segregation:** Allow specialized configuration for DB connection pools (HikariCP) and thread pools tailored to the specific needs of API (latency) vs. Batch (throughput).
- **Failure Isolation:** Ensure that runtime failures (e.g., OutOfMemoryError in a Batch Job) do not impact the availability of the API server.
- **Independent Scalability:** Enable independent deployment and scaling of API and Batch components in a Kubernetes environment.
- **Build Performance:** Optimize build and test execution times by leveraging Gradle's incremental build capabilities.

## 3. Module Architecture (Pragmatic Hexagonal)

### stockwellness-core (Library JAR)
- **Role:** The "Single Source of Truth" for domain logic and common infrastructure.
- **Dependencies:** None (internal).
- **Contents:**
    - **Domain:** JPA Entities, Value Objects, Domain Services.
    - **Application:** Service Interfaces (Input Ports), Use Case implementations.
    - **Infrastructure:** JPA Repositories, Redis/Kafka clients, and shared configurations.
- **Build:** `bootJar.enabled = false`, `jar.enabled = true`.

### stockwellness-api (Executable JAR)
- **Role:** Web Adapter for external REST requests.
- **Dependencies:** `implementation(project(":stockwellness-core"))`.
- **Contents:** Controllers, DTOs, Security (OAuth2), Global Exception Handlers, and Web-specific configs.

### stockwellness-batch (Executable JAR)
- **Role:** Batch Adapter for heavy-duty data processing and scheduling.
- **Dependencies:** `implementation(project(":stockwellness-core"))`.
- **Contents:** Spring Batch Job/Step configurations, Reader/Processor/Writer implementations, and Scheduler logic.

## 4. Technical Requirements
- **Configuration Management:** 
    - Use `spring.config.import` to load a base `application-core.yaml` from the `core` module.
    - Allow `api` and `batch` to override settings (e.g., `hikari.maximum-pool-size`) in their local `application.yaml`.
- **Testing Strategy:**
    - **Localized Tests:** Unit and integration tests reside within their respective modules.
    - **Shared Fixtures:** Common test utilities (Mocks, TestContainers base) moved to `core/src/testFixtures`.
    - **Coverage:** Maintain >80% coverage per module.
- **Build System:** Refactor `build.gradle.kts` and `settings.gradle.kts` to support the multi-module structure.

## 5. Acceptance Criteria
- [ ] Project successfully builds with `./gradlew build`.
- [ ] `stockwellness-api` and `stockwellness-batch` produce executable JARs.
- [ ] `stockwellness-core` produces a plain library JAR.
- [ ] All existing tests pass after migration.
- [ ] API can be started independently and connects to the DB using its specific profile.
- [ ] Batch can be started independently and handles its scheduled tasks.
- [ ] No circular dependencies between modules.

## 6. Out of Scope
- Splitting infrastructure into a separate `stockwellness-infra` module (deferred for future refactoring).
- Migrating to a full Microservices Architecture (MSA) at this stage.
