# Specification: Batch Jobs Refactoring (Stock Price & Sector Sync)

## Overview
This track focuses on refactoring the core Spring Batch jobs for stock price collection and sector insight synchronization. The goal is to resolve critical mathematical and performance issues identified in previous reviews, align the implementation with Hexagonal Architecture principles, and optimize data persistence and caching strategies.

## Functional Requirements
### 1. Stock Price Batch (`fetch-prices`) Refactoring
- **Persistence Optimization:** Transition to high-performance bulk insertion methods (e.g., `JdbcBatchItemWriter`) to handle large datasets efficiently.
- **Feature Expansion:** Enhance the batch to support additional metrics or market data requirements as defined in the product roadmap.
- **Gap Handling:** Ensure robust logic for filling historical data gaps without redundant API calls.

### 2. Sector Insight Batch (`runSectorSync`) Refactoring
- **Performance & Correctness:** 
    - Fix data ordering bugs (ensuring chronological ASC order for technical indicator calculations).
    - Resolve N+1 API and DB query bottlenecks through pre-fetching and batching.
- **Architectural Alignment:** 
    - Decouple batch orchestration from business logic by moving analysis routines into core Domain Services.
    - Ensure strict separation between Batch Adapters and Core Domain logic.
- **Caching & Pre-warming:** 
    - Implement robust cache eviction logic in Job Listeners.
    - Enhance proactive cache pre-warming to ensure sub-100ms response times for end-users.

## Non-Functional Requirements
- **Stability:** Implement resilient retry mechanisms for external KIS API integrations.
- **Maintainability:** Improve code clarity and remove dead/redundant logic.
- **Observability:** Enhance logging for batch progress and error diagnostics.

## Acceptance Criteria
- Successful execution of both batches without rate-limit errors or data corruption.
- Technical indicators (RSI, MA) calculated with correct chronological data.
- Domain services established as the primary location for business logic.
- Cache pre-warming successfully populates Redis after batch completion.

## Out of Scope
- Implementation of new AI analysis features (beyond data structuring).
- Front-end dashboard enhancements.
