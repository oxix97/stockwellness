# Change summary: Implementation of a Kafka-based event-driven architecture to decouple batch processing from API side-effects, including conditional event publishing and sequential job orchestration.

The overall architecture is solid and follows Hexagonal principles. However, the use of a singleton component for collecting updated symbols in a batch listener introduces a critical state-sharing bug if multiple jobs run concurrently. Additionally, the Redis cache invalidation logic could be optimized to handle large batches more efficiently.

## File: stockwellness-batch/src/main/java/org/stockwellness/batch/job/stock/price/StockPriceSyncEventListener.java
### L24: [HIGH] Thread-safety violation and state sharing across concurrent job executions.
The `updatedSymbols` set is a member variable of a singleton `@Component`. If a manual job execution via `BatchAdminController` overlaps with a scheduled execution from `StockwellnessScheduler`, both jobs will write to and clear the same set, leading to corrupted event data.

Suggested change:
```java
- @Component
- @RequiredArgsConstructor
- public class StockPriceSyncEventListener implements ItemWriteListener<List<StockPrice>>, JobExecutionListener {
-
-     private final KafkaEventPublisher kafkaEventPublisher;
-     private final Set<String> updatedSymbols = ConcurrentHashMap.newKeySet();

+ @Component
+ @JobScope
+ @RequiredArgsConstructor
+ public class StockPriceSyncEventListener implements ItemWriteListener<List<StockPrice>>, JobExecutionListener {
+
+     private final KafkaEventPublisher kafkaEventPublisher;
+     private final Set<String> updatedSymbols = ConcurrentHashMap.newKeySet();
```
*Note: Adding `@JobScope` ensures a new instance of the listener is created for every job execution, isolating the state.*

## File: stockwellness-api/src/main/java/org/stockwellness/adapter/in/kafka/StockPriceUpdateConsumer.java
### L43: [MEDIUM] Inefficient O(N) cache eviction in a loop.
Calling `cache.evict` for every symbol individually results in multiple round-trips to Redis. For a batch of 2000+ stocks, this could significantly latency the consumer thread.

Suggested change:
```java
    private void invalidateCaches(StockPriceUpdatedEvent event) {
        int currentYear = LocalDate.now().getYear();
        
        // Use a pipelined approach or batch delete if supported by the cache provider
        Optional.ofNullable(cacheManager.getCache("stock_prices")).ifPresent(cache -> {
+           // If using Redis, consider using a custom repository method to delete by pattern 
+           // or collect keys and use native commands if performance becomes a bottleneck.
            for (String symbol : event.symbols()) {
                cache.evict(symbol + ":" + currentYear);
                cache.evict(symbol + ":" + (currentYear - 1));
            }
        });
```

## File: stockwellness-batch/src/main/java/org/stockwellness/batch/StockwellnessScheduler.java
### L35: [MEDIUM] Lack of job status verification before proceeding to the next step.
The scheduler assumes that if `jobLauncher.run` doesn't throw an exception, the job was successful. In Spring Batch, a job can end with `BatchStatus.FAILED` without throwing an exception to the launcher.

Suggested change:
```java
-            jobLauncher.run(stockMasterSyncJob, masterParams);
+            JobExecution masterExecution = jobLauncher.run(stockMasterSyncJob, masterParams);
+            if (masterExecution.getStatus() != BatchStatus.COMPLETED) {
+                throw new RuntimeException("Stock Master Sync failed with status: " + masterExecution.getStatus());
+            }
```

## File: stockwellness-batch/src/main/java/org/stockwellness/batch/BatchAdminController.java
### L155: [LOW] Unbounded thread pool usage for batch jobs.
`CompletableFuture.runAsync` uses the `ForkJoinPool.commonPool()` by default. Long-running batch jobs can saturate this pool, affecting other parts of the application that rely on it.

Suggested change:
```java
-    private String launchJobAsync(Job job, JobParameters params) {
-        CompletableFuture.runAsync(() -> {
+    private String launchJobAsync(Job job, JobParameters params) {
+        // Use the existing kisBatchExecutor or a dedicated TaskExecutor
+        CompletableFuture.runAsync(() -> {
             try {
                 jobLauncher.run(job, params);
             } catch (Exception e) {
                 log.error("Job {} failed", job.getName(), e);
             }
-        });
+        }, kisBatchExecutor);
```
