# Implementation Plan: Advanced Chart & Price API

This plan builds upon the existing `Price & Chart Engine` to add advanced data points (VWAP, Fundamentals, Benchmarking) and high-performance caching for large historical datasets.

## Phase 1: Domain & Port Enhancement (Advanced Features)
Focus on extending the domain models and defining the contracts for new data sources.

- [ ] Task: Update Domain Models & DTOs
    - [ ] Add `vwap`, `peRatio`, and `pbRatio` fields to `StockPriceResult`.
    - [ ] Create `BenchmarkSeries` DTO to hold time-aligned index data.
    - [ ] Update `ChartDataResponse` to include the `benchmark` series and enhanced data points.
- [ ] Task: Define New Output Ports
    - [ ] Create `LoadFundamentalPort` to fetch historical P/E and P/B data.
    - [ ] Create `LoadBenchmarkSeriesPort` with support for date-range alignment.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Domain & Port Enhancement' (Protocol in workflow.md)

## Phase 2: Adapter Layer - Data Alignment & Fundamentals
Implement the persistence logic for fundamentals and the benchmarking alignment service.

- [ ] Task: Implement `FundamentalAdapter` (QueryDSL)
    - [ ] TDD: Create repository tests for fetching P/E and P/B ratios.
    - [ ] Implement QueryDSL logic to retrieve fundamental data mapped to dates.
- [ ] Task: Implement Calendar Standardization Logic
    - [ ] TDD: Write tests for "Forward-fill" logic across different market calendars (e.g., KRX vs. NYSE).
    - [ ] Implement the alignment service that merges stock and benchmark series onto a common date axis.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Adapter Layer - Data Alignment & Fundamentals' (Protocol in workflow.md)

## Phase 3: High-Performance Caching & Serialization
Implement the optimized Redis caching strategy using Zstd/LZ4 and Jackson Afterburner.

- [ ] Task: Configure Advanced Redis Serialization
    - [ ] Integrate `Zstd` or `LZ4` compression into the `RedisTemplate` configuration.
    - [ ] Enable `Jackson Afterburner/Blackbird` modules for the ObjectMapper.
- [ ] Task: Implement "Lazy-Loaded Tiered Cache" with Proactive Warm-up
    - [ ] Implement logic to store "All" period history as a compressed blob.
    - [ ] Create a Kafka consumer for the `market-data-updated` event.
    - [ ] Implement the proactive cache warm-up service triggered by the Kafka event.
- [ ] Task: TDD - Cache Performance & Integrity
    - [ ] Write tests to verify compression/decompression integrity.
    - [ ] Verify that the Kafka event correctly triggers a cache update.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: High-Performance Caching & Serialization' (Protocol in workflow.md)

## Phase 4: Application Layer - Service Integration
Integrate all components into the `StockChartService` using Virtual Threads.

- [ ] Task: Implement Enhanced `StockChartService` (TDD)
    - [ ] Write tests for the integrated flow (Price + VWAP + Fundamentals + Benchmark).
    - [ ] Implement the service logic using Java 21 Virtual Threads (`StructuredTaskScope` or `CompletableFuture`) for parallel data fetching.
    - [ ] Ensure VWAP calculation logic is correctly applied during aggregation.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Application Layer - Service Integration' (Protocol in workflow.md)

## Phase 5: API Layer & Performance Validation
Expose the new features via REST and validate the <100ms performance target.

- [ ] Task: Update `StockDiscoveryController` / `StockChartController`
    - [ ] Add the new data fields to the API response.
    - [ ] Update Spring REST Docs with the enhanced response structure.
- [ ] Task: Performance Benchmarking
    - [ ] TDD/Performance: Execute load tests for "3Y" and "All" periods to verify sub-100ms response times.
    - [ ] Verify Zstd/LZ4 throughput gains.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: API Layer & Performance Validation' (Protocol in workflow.md)
