# Specification: Advanced Chart & Price API

## Overview
This track focuses on enhancing the core `Price & Chart Engine` to provide high-performance, data-rich historical price series. We will implement advanced data points (VWAP, Fundamentals, Benchmarking) and optimize for sub-100ms response times for large datasets (3Y/All periods) using aggressive caching and high-efficiency serialization.

## Functional Requirements
- **Latest Close Inquiry:** Retrieve the most recent End-Of-Day (EOD) data for a specific stock ticker.
- **Period-based Chart Data:** Support OHLCV retrieval for 1W, 1M, 3M, 1Y, 3Y, and "All" (Full History) periods.
- **Enhanced Data Points:**
    - **VWAP:** Calculate and include Volume-Weighted Average Price for each period in the series.
    - **Fundamental Overlays:** Include historical P/E and P/B ratios mapped to the price time series.
    - **Dynamic Benchmarking:** Provide a separate, time-aligned price series for the stock's corresponding market index (KOSPI/KOSDAQ).
- **Calendar Standardization (Data Alignment):** 
    - Ensure stock and benchmark series share a common date axis.
    - Handle market-specific holidays (e.g., KRX vs. NYSE) by **Forward-filling** (using the previous day's close) for missing data points on non-trading days to prevent visualization misalignment.

## Non-Functional Requirements
- **Performance:** Response time for "3Y" and "All" historical data requests must be under **100ms**.
- **Caching & Warm-up Strategy:** 
    - Implement a "Lazy-Loaded Tiered Cache" in Redis for "All" period history.
    - **Proactive Invalidation:** Redis cache must be proactively updated/warmed-up upon receiving a Kafka `market-data-updated` event, ensuring fresh data is ready before the first user request.
- **Serialization & Efficiency:**
    - Use **Zstd** or **LZ4** compression for Redis blobs to maximize I/O performance.
    - Utilize **Jackson Afterburner/Blackbird** modules to minimize serialization overhead during the retrieval process.
- **Scalability:** Leverage Java 21 Virtual Threads for non-blocking I/O when fetching multi-source data (Price + Benchmarks + Fundamentals).

## Acceptance Criteria
- API returns OHLCV + VWAP + P/E + P/B for all requested periods.
- Response includes a `benchmark` field containing a date-aligned index series (verified for holiday alignment).
- "All" period requests consistently return in <100ms.
- Integration tests verify that the Kafka event triggers a successful cache warm-up.
- Performance tests confirm that Zstd/LZ4 compression meets throughput targets.

## Out of Scope
- Real-time/Intraday price data (EOD only).
- Advanced technical indicators (RSI, MACD) - these are handled by separate tracks.
- Frontend visualization implementation.
