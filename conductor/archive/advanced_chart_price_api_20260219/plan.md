# Implementation Plan: Advanced Chart & Price API

This plan builds upon the existing `Price & Chart Engine` to add advanced data points (VWAP, Fundamentals, Benchmarking) and high-performance caching for large historical datasets.

## Phase 1: Domain & Port Enhancement (Advanced Features)
Focus on extending the domain models and defining the contracts for new data sources.

- [x] Task: Update Domain Models & DTOs
    - [x] Add `vwap`, `peRatio`, and `pbRatio` fields to `StockPriceResult`.
    - [x] Create `BenchmarkSeries` DTO to hold time-aligned index data.
    - [x] Update `ChartDataResponse` to include the `benchmark` series and enhanced data points.
- [x] Task: Define New Output Ports
    - [x] Create `LoadFundamentalPort` to fetch historical P/E and P/B data.
    - [x] Create `LoadBenchmarkSeriesPort` with support for date-range alignment.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Domain & Port Enhancement' (Protocol in workflow.md)

## Phase 2: Adapter Layer - Data Alignment & Fundamentals
Implement the persistence logic for fundamentals and the benchmarking alignment service.

- [x] Task: Implement `FundamentalAdapter` (QueryDSL)
    - [x] TDD: Create repository tests for fetching P/E and P/B ratios.
    - [x] Implement QueryDSL logic to retrieve fundamental data mapped to dates.
- [x] Task: Implement Calendar Standardization Logic
    - [x] TDD: Write tests for "Forward-fill" logic across different market calendars (e.g., KRX vs. NYSE).
    - [x] Implement the alignment service that merges stock and benchmark series onto a common date axis.
- [x] Task: Conductor - User Manual Verification 'Phase 2: Adapter Layer - Data Alignment & Fundamentals' (Protocol in workflow.md)

## Phase 3: High-Performance Caching & Serialization
Implement the optimized Redis caching strategy using Zstd/LZ4 and Jackson Afterburner.

- [x] Task: Configure Advanced Redis Serialization
    - [x] Integrate `Zstd` or `LZ4` compression into the `RedisTemplate` configuration.
    - [x] Enable `Jackson Afterburner/Blackbird` modules for the ObjectMapper.
- [x] Task: Implement "Lazy-Loaded Tiered Cache" with Proactive Warm-up
    - [x] Implement logic to store "All" period history as a compressed blob.
    - [x] Create a Kafka consumer for the `market-data-updated` event.
    - [x] Implement the proactive cache warm-up service triggered by the Kafka event.
- [x] Task: TDD - Cache Performance & Integrity
    - [x] Write tests to verify compression/decompression integrity.
    - [x] Verify that the Kafka event correctly triggers a cache update.
- [x] Task: Conductor - User Manual Verification 'Phase 3: High-Performance Caching & Serialization' (Protocol in workflow.md)

## Phase 4: Application Layer - Service Integration
Integrate all components into the `StockChartService` using Virtual Threads.

- [x] Task: Implement Enhanced `StockChartService` (TDD)
    - [x] Write tests for the integrated flow (Price + VWAP + Fundamentals + Benchmark).
    - [x] Implement the service logic using Java 21 Virtual Threads (`StructuredTaskScope` or `CompletableFuture`) for parallel data fetching.
    - [x] Ensure VWAP calculation logic is correctly applied during aggregation.
- [x] Task: Conductor - User Manual Verification 'Phase 4: Application Layer - Service Integration' (Protocol in workflow.md)

## Phase 5: API Layer & Performance Validation
Expose the new features via REST and validate the <100ms performance target.

- [x] Task: Update `StockDiscoveryController` / `StockChartController`
    - [x] Add the new data fields to the API response.
    - [x] Update Spring REST Docs with the enhanced response structure.
- [x] Task: Performance Benchmarking
    - [x] TDD/Performance: Execute load tests for "3Y" and "All" periods to verify sub-100ms response times.
    - [x] Verify Zstd/LZ4 throughput gains.
- [x] Task: Conductor - User Manual Verification 'Phase 5: API Layer & Performance Validation' (Protocol in workflow.md)
