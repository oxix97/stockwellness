# Implementation Plan: Multi-Module Migration

This plan follows an incremental approach to refactor the Stockwellness project into a multi-module Gradle structure (`core`, `api`, `batch`), ensuring each step is verified through tests.

## Phase 1: Foundation & Core Module Setup
- [x] Task: Refactor Gradle Root Configuration
    - [x] Update `settings.gradle.kts` to include `stockwellness-core`, `stockwellness-api`, and `stockwellness-batch`.
    - [x] Refactor root `build.gradle.kts` to use `subprojects` and `allprojects` blocks for shared plugins and dependencies.
- [x] Task: Create Module Directory Structure
    - [x] Create directories: `stockwellness-core`, `stockwellness-api`, `stockwellness-batch`.
    - [x] Initialize `build.gradle.kts` for each module.
- [x] Task: Migrate Domain, Application, and Infrastructure to `core`
    - [x] Move `org.stockwellness.domain`, `org.stockwellness.application`, and `org.stockwellness.adapter.out` (JPA/Redis) to `stockwellness-core`.
    - [x] Move common configs (JPA, Redis, QueryDSL) to `stockwellness-core`.
    - [x] Move `src/main/resources/application.yaml` to `stockwellness-core` as `application-core.yaml`.
- [x] Task: Setup Shared Test Fixtures
    - [x] Enable `java-test-fixtures` plugin in `stockwellness-core`.
    - [x] Move common test utilities and base classes (e.g., `RedisIntegrationTest`, `QueryDslTest`) to `src/testFixtures`.
- [x] Task: Verify Core Module
    - [x] Write/Fix unit tests for migrated domain logic in `core`.
    - [x] Run `./gradlew :stockwellness-core:test` and ensure >80% coverage.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Foundation' (Protocol in workflow.md)

## Phase 2: API Module Migration
- [x] Task: Migrate Web Adapter Components
    - [x] Move `org.stockwellness.adapter.in.web` (Controllers, DTOs) to `stockwellness-api`.
    - [x] Move `org.stockwellness.global.security` and `org.stockwellness.config` (Web/Security related) to `stockwellness-api`.
- [x] Task: Configure API Properties
    - [x] Create `stockwellness-api/src/main/resources/application.yaml`.
    - [x] Implement `spring.config.import: optional:classpath:application-core.yaml`.
    - [x] Configure API-specific HikariCP settings (short timeouts, high concurrency).
- [x] Task: Verify API Module
    - [x] Write/Fix unit and integration tests for Controllers and Security.
    - [x] Run `./gradlew :stockwellness-api:test` and ensure >80% coverage.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: API Migration' (Protocol in workflow.md)

## Phase 3: Batch Module Migration
- [ ] Task: Migrate Batch Adapter Components
    - [ ] Move `org.stockwellness.batch` (Jobs, Steps, Admin Controllers) to `stockwellness-batch`.
- [ ] Task: Configure Batch Properties
    - [ ] Create `stockwellness-batch/src/main/resources/application.yaml`.
    - [ ] Implement `spring.config.import: optional:classpath:application-core.yaml`.
    - [ ] Configure Batch-specific HikariCP settings (long timeouts, optimized for throughput).
- [ ] Task: Verify Batch Module
    - [ ] Write/Fix unit and integration tests for Batch Jobs.
    - [ ] Run `./gradlew :stockwellness-batch:test` and ensure >80% coverage.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Batch Migration' (Protocol in workflow.md)

## Phase 4: Final Integration & Cleanup
- [ ] Task: Final Build & Artifact Verification
    - [ ] Run `./gradlew clean build` from root.
    - [ ] Verify `stockwellness-api.jar` and `stockwellness-batch.jar` are executable.
    - [ ] Verify `stockwellness-core.jar` is a plain library JAR.
- [ ] Task: Cleanup Orphaned Files
    - [ ] Remove unused code and resources from the root `src` directory.
    - [ ] Update `README.md` or `GUIDE.md` to reflect the new module structure.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Final Integration' (Protocol in workflow.md)
