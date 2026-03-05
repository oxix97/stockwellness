# Implementation Plan: Kafka Event-Driven Architecture Foundation

## Phase 1: Infrastructure & Core Setup
- [x] Task: Update `compose.yaml` to include Kafka and Zookeeper services.
- [x] Task: Add `spring-kafka` dependency to `stockwellness-core`, `stockwellness-batch`, and `stockwellness-api`.
- [x] Task: Define `StockPriceUpdatedEvent` in `stockwellness-core` (Domain Event).
- [x] Task: Configure Kafka Common Properties (Bootstrap servers, Serializers) in `application.yml` (or module-specific configs).
- [x] Task: Conductor - User Manual Verification 'Infrastructure & Core Setup' (Protocol in workflow.md)

## Phase 2: Event Production (stockwellness-batch)
- [x] Task: Implement `KafkaEventPublisher` in `stockwellness-batch` using `KafkaTemplate`.
- [x] Task: Create integration test for `KafkaEventPublisher` (using Testcontainers or Embedded Kafka).
- [x] Task: Integrate `KafkaEventPublisher` into the `StockPriceSync` batch job completion listener.
- [x] Task: Conductor - User Manual Verification 'Event Production' (Protocol in workflow.md)

## Phase 3: Event Consumption (stockwellness-api)
- [x] Task: Configure Kafka Consumer properties in `stockwellness-api`.
- [x] Task: Implement `StockPriceUpdateConsumer` with `@KafkaListener`.
- [x] Task: Implement Cache Invalidation logic within the consumer (Targeting Redis).
- [x] Task: Implement AI Insight Trigger logic (Placeholder or preliminary service call).
- [x] Task: Create integration test for `StockPriceUpdateConsumer`.
- [x] Task: Conductor - User Manual Verification 'Event Consumption' (Protocol in workflow.md)

## Phase 4: End-to-End Verification
- [x] Task: Run full local environment (Batch + API + Kafka) and verify event flow.
- [x] Task: Verify Redis cache is correctly invalidated after a mock batch run.
- [x] Task: Verify AI trigger is logged/executed.
- [x] Task: Conductor - User Manual Verification 'End-to-End Verification' (Protocol in workflow.md)

## Phase 5: Advanced Control & Scheduling
- [x] Task: Update `StockPriceSyncEventListener` to conditionally publish events based on `JobParameters`.
- [x] Task: Create `StockwellnessScheduler` to orchestrate Sequential Daily Batch (Master -> Price -> Sector).
- [x] Task: Update `BatchAdminController` to allow optional event publishing.
- [x] Task: Conductor - User Manual Verification 'Advanced Control' (Protocol in workflow.md)

