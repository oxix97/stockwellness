# Implementation Plan: Multi-Module Migration

This plan follows an incremental approach to refactor the Stockwellness project into a multi-module Gradle structure (`core`, `api`, `batch`), ensuring each step is verified through tests.

## Phase 1: Foundation & Core Module Setup
- [ ] Task: Refactor Gradle Root Configuration
    - [ ] Update `settings.gradle.kts` to include `stockwellness-core`, `stockwellness-api`, and `stockwellness-batch`.
    - [ ] Refactor root `build.gradle.kts` to use `subprojects` and `allprojects` blocks for shared plugins and dependencies.
- [ ] Task: Create Module Directory Structure
    - [ ] Create directories: `stockwellness-core`, `stockwellness-api`, `stockwellness-batch`.
    - [ ] Initialize `build.gradle.kts` for each module.
- [ ] Task: Migrate Domain, Application, and Infrastructure to `core`
    - [ ] Move `org.stockwellness.domain`, `org.stockwellness.application`, and `org.stockwellness.adapter.out` (JPA/Redis) to `stockwellness-core`.
    - [ ] Move common configs (JPA, Redis, QueryDSL) to `stockwellness-core`.
    - [ ] Move `src/main/resources/application.yaml` to `stockwellness-core` as `application-core.yaml`.
- [ ] Task: Setup Shared Test Fixtures
    - [ ] Enable `java-test-fixtures` plugin in `stockwellness-core`.
    - [ ] Move common test utilities and base classes (e.g., `RedisIntegrationTest`, `QueryDslTest`) to `src/testFixtures`.
- [ ] Task: Verify Core Module
    - [ ] Write/Fix unit tests for migrated domain logic in `core`.
    - [ ] Run `./gradlew :stockwellness-core:test` and ensure >80% coverage.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Foundation' (Protocol in workflow.md)

## Phase 2: API Module Migration
- [ ] Task: Migrate Web Adapter Components
    - [ ] Move `org.stockwellness.adapter.in.web` (Controllers, DTOs) to `stockwellness-api`.
    - [ ] Move `org.stockwellness.global.security` and `org.stockwellness.config` (Web/Security related) to `stockwellness-api`.
- [ ] Task: Configure API Properties
    - [ ] Create `stockwellness-api/src/main/resources/application.yaml`.
    - [ ] Implement `spring.config.import: optional:classpath:application-core.yaml`.
    - [ ] Configure API-specific HikariCP settings (short timeouts, high concurrency).
- [ ] Task: Verify API Module
    - [ ] Write/Fix unit and integration tests for Controllers and Security.
    - [ ] Run `./gradlew :stockwellness-api:test` and ensure >80% coverage.
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
