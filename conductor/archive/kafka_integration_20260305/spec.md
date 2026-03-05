# Track Specification: Kafka Event-Driven Architecture Foundation

## 1. Overview
Establish a robust Kafka-based messaging backbone to decouple the `stockwellness-batch` and `stockwellness-api` modules. This track focuses on enabling event-driven responses to stock price updates, specifically for cache management and AI-driven insights.

## 2. Functional Requirements
### 2.1 Event Definition (`stockwellness-core`)
- Define a domain event `StockPriceUpdatedEvent` containing stock symbols and the update timestamp.
- Ensure the event is serializable for Kafka transmission (JSON format).

### 2.2 Event Production (`stockwellness-batch`)
- Integrate a Kafka Producer using `KafkaTemplate`.
- Trigger the event emission at the end of the `StockPriceSync` batch job.
- Group multiple stock updates into a single event or batch of events to optimize throughput.

### 2.3 Event Consumption (`stockwellness-api`)
- Implement a Kafka Consumer using `@KafkaListener`.
- **Scenario A: Cache Invalidation:** On receipt of `StockPriceUpdatedEvent`, invalidate relevant Redis caches for the affected stock symbols.
- **Scenario B: AI Insights Trigger:** Initiate a background task to generate/refresh AI market reports for the updated stocks.

## 3. Tech Stack & Infrastructure
- **Infrastructure:** Add Kafka and Zookeeper services to the local `compose.yaml`.
- **Integration:** Use `spring-kafka` (Direct integration) for granular control.
- **Serialization:** Use Jackson-based JSON serialization for Kafka messages.

## 4. Acceptance Criteria
- [ ] Kafka and Zookeeper containers are running and accessible.
- [ ] `stockwellness-batch` successfully produces `StockPriceUpdatedEvent` after a sync job.
- [ ] `stockwellness-api` successfully consumes the event and performs a log-verified cache invalidation.
- [ ] AI report generation logic is triggered (mocked or preliminary implementation).

## 5. Out of Scope
- Implementing the "Portfolio Rebalancing Notifications" (deferred to a future track).
- Production-grade Kafka tuning (e.g., partition strategy, security/TLS).
- Migration of all existing internal events to Kafka.
