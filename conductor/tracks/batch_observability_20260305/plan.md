# Implementation Plan: Batch Observability & API Feedback Enhancement

## Phase 1: Logging Infrastructure (MDC)
- [x] Task: Implement `BatchMdcListener` to manage MDC context (JobName, ExecutionId).
- [x] Task: Register `BatchMdcListener` to all Batch Jobs in `StockMasterSyncJobConfig`, `StockPriceBatchConfig`, etc.
- [x] Task: Update `logback-spring.xml` to include MDC variables in the log pattern.

## Phase 2: API Enhancement
- [x] Task: Create `BatchExecutionResponse` DTO for standardized API feedback.
- [x] Task: Refactor `BatchAdminController` to return `ResponseEntity<BatchExecutionResponse>`.
- [x] Task: Implement URL generation logic for status tracking.

## Phase 3: Message Standardization
- [ ] Task: Define a common log message template for batch events.
- [ ] Task: Refactor existing listeners (`StockPriceProgressListener`, etc.) to use the standardized template.

## Phase 4: Verification
- [ ] Task: Verify log traceability by running multiple jobs concurrently.
- [ ] Task: Verify API response format using `curl` or Postman.
- [ ] Task: Conductor - User Manual Verification 'Batch Observability'
