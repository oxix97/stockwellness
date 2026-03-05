# Implementation Plan: Kafka Event-Driven Architecture Foundation

## Phase 1: Infrastructure & Core Setup
- [ ] Task: Update `compose.yaml` to include Kafka and Zookeeper services.
- [ ] Task: Add `spring-kafka` dependency to `stockwellness-core`, `stockwellness-batch`, and `stockwellness-api`.
- [ ] Task: Define `StockPriceUpdatedEvent` in `stockwellness-core` (Domain Event).
- [ ] Task: Configure Kafka Common Properties (Bootstrap servers, Serializers) in `application.yml` (or module-specific configs).
- [ ] Task: Conductor - User Manual Verification 'Infrastructure & Core Setup' (Protocol in workflow.md)

## Phase 2: Event Production (stockwellness-batch)
- [ ] Task: Implement `KafkaEventPublisher` in `stockwellness-batch` using `KafkaTemplate`.
- [ ] Task: Create integration test for `KafkaEventPublisher` (using Testcontainers or Embedded Kafka).
- [ ] Task: Integrate `KafkaEventPublisher` into the `StockPriceSync` batch job completion listener.
- [ ] Task: Conductor - User Manual Verification 'Event Production' (Protocol in workflow.md)

## Phase 3: Event Consumption (stockwellness-api)
- [ ] Task: Configure Kafka Consumer properties in `stockwellness-api`.
- [ ] Task: Implement `StockPriceUpdateConsumer` with `@KafkaListener`.
- [ ] Task: Implement Cache Invalidation logic within the consumer (Targeting Redis).
- [ ] Task: Implement AI Insight Trigger logic (Placeholder or preliminary service call).
- [ ] Task: Create integration test for `StockPriceUpdateConsumer`.
- [ ] Task: Conductor - User Manual Verification 'Event Consumption' (Protocol in workflow.md)

## Phase 4: End-to-End Verification
- [ ] Task: Run full local environment (Batch + API + Kafka) and verify event flow.
- [ ] Task: Verify Redis cache is correctly invalidated after a mock batch run.
- [ ] Task: Verify AI trigger is logged/executed.
- [ ] Task: Conductor - User Manual Verification 'End-to-End Verification' (Protocol in workflow.md)
