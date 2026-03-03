# Implementation Plan: Batch Jobs Refactoring

This plan aims to refactor the core batch synchronization jobs to improve performance, mathematical correctness, and architectural alignment with Hexagonal Architecture.

## Phase 1: Architectural Alignment & Domain Logic
Focus on moving business logic from the batch layer to the core domain services and ensuring ports support efficient operations.

- [x] Task: Relocate Sector Analysis Logic to Core Domain
    - [x] Write unit tests for `SectorAnalysisService` (covering indicator calculations and status logic)
    - [x] Implement `SectorAnalysisService` in the `stockwellness-core` module
- [x] Task: Refactor Ports and Persistence Adapters
    - [x] Update `SectorInsightPort` to include bulk data retrieval methods (N+1 prevention)
    - [x] Implement bulk fetching in `SectorPersistenceAdapter` using QueryDSL
- [x] Task: Conductor - User Manual Verification 'Phase 1: Architectural Alignment & Domain Logic' (Protocol in workflow.md)

## Phase 2: Optimized Stock Price Sync (`fetch-prices`)
Refactor the price synchronization batch to use high-performance persistence and support expanded data points.

- [x] Task: Implement High-Performance Bulk Writing
    - [x] Write integration tests for `JdbcBatchItemWriter` implementation
    - [x] Configure `JdbcBatchItemWriter` in `StockPriceBatchConfig` to replace JPA-based writing
- [x] Task: Enhance Price Collection Logic
    - [x] Refactor `StockPriceProcessor` to pre-fetch historical closing prices for all stocks in a chunk
    - [x] Add support for "Previous Close Price" and ensure it's correctly mapped from API/DB
- [x] Task: Conductor - User Manual Verification 'Phase 2: Optimized Stock Price Sync' (Protocol in workflow.md)

## Phase 3: Correct & Performant Sector Sync (`runSectorSync`)
Address critical bugs related to data ordering and API rate limits while optimizing execution.

- [x] Task: Resolve Data Ordering & Mathematical Errors
    - [x] Update `KisSectorAdapter` or `SectorInsightItemProcessor` to ensure chronological (ASC) data delivery
    - [x] Verify RSI and MA calculations with TDD after fixing data order
- [x] Task: Implement API Rate Limiting & N+1 Fixes
    - [x] Add controlled delays (`Thread.sleep`) or a proper rate limiter to `SectorInsightItemProcessor`
    - [x] Refactor `Processor` to use bulk-fetched metadata instead of per-sector DB calls
- [x] Task: Refine Caching & Pre-warming
    - [x] Update `SectorEodJobListener` with safe, `Optional`-based cache eviction
    - [x] Implement proactive pre-warming for ranking and supply APIs
- [x] Task: Conductor - User Manual Verification 'Phase 3: Correct & Performant Sector Sync' (Protocol in workflow.md)

## Phase 4: Final Validation & Integration
Ensure the refactored system works seamlessly and remove redundant code.

- [x] Task: End-to-End Batch Validation
    - [x] Run the complete `stockPriceBatchJob` and verify data integrity
    - [x] Monitor logs for rate-limit warnings or performance bottlenecks
- [x] Task: Cleanup & Documentation
    - [x] Delete deprecated synchronization logic in `SectorInsightService`
    - [x] Update API documentation if response structures were slightly modified
- [x] Task: Conductor - User Manual Verification 'Phase 4: Final Validation & Integration' (Protocol in workflow.md)
