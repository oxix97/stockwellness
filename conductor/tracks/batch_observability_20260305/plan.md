# Implementation Plan: Batch Observability & API Feedback Enhancement

## Phase 1: Logging Infrastructure (MDC)
- [ ] Task: Implement `BatchMdcListener` to manage MDC context (JobName, ExecutionId).
- [ ] Task: Register `BatchMdcListener` to all Batch Jobs in `StockMasterSyncJobConfig`, `StockPriceBatchConfig`, etc.
- [ ] Task: Update `logback-spring.xml` to include MDC variables in the log pattern.

## Phase 2: API Enhancement
- [ ] Task: Create `BatchExecutionResponse` DTO for standardized API feedback.
- [ ] Task: Refactor `BatchAdminController` to return `ResponseEntity<BatchExecutionResponse>`.
- [ ] Task: Implement URL generation logic for status tracking.

## Phase 3: Message Standardization
- [ ] Task: Define a common log message template for batch events.
- [ ] Task: Refactor existing listeners (`StockPriceProgressListener`, etc.) to use the standardized template.

## Phase 4: Verification
- [ ] Task: Verify log traceability by running multiple jobs concurrently.
- [ ] Task: Verify API response format using `curl` or Postman.
- [ ] Task: Conductor - User Manual Verification 'Batch Observability'
