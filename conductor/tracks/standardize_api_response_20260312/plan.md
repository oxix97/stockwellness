# Implementation Plan: Standardizing API Response and Global Exception Handling (plan.md)

## Phase 1: Core Foundation (stockwellness-core) [x]
Building the foundational components in the core module for global use.

- [x] Task: Create `ApiResponse<T>` record in `org.stockwellness.global.common`.
- [x] Task: Create `ErrorCode` Enum with fields for HTTP Status, custom code, and default message.
- [x] Task: Create `ErrorResponse` record in `org.stockwellness.global.error` including a list of `FieldError` records.
- [x] Task: Implement `BusinessException` as an abstract class and migrate common domain exceptions to use it.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Core Foundation' (Protocol in workflow.md)

## Phase 2: Global Handler Enhancement (stockwellness-api) [x]
Implementing the logic to catch exceptions and return standardized responses.

- [x] Task: Implement `GlobalExceptionHandler` using `@RestControllerAdvice`.
- [x] Task: Use Java 21 `switch pattern matching` in `GlobalExceptionHandler` to handle different exception types.
- [x] Task: Implement logic to handle `MethodArgumentNotValidException` and populate `FieldError` details.
- [x] Task: Integrate `Trace ID` (MDC or request header) into the `ErrorResponse` generation.
- [x] Task: Conductor - User Manual Verification 'Phase 2: Global Handler Enhancement' (Protocol in workflow.md)

## Phase 3: Adoption and Validation (Integration) [x]
Updating existing controllers and verifying the new structure.

- [x] Task: Update `PortfolioController` and `MemberController` to use `ApiResponse<T>` as the return type.
- [x] Task: Create integration tests to verify both success and error response formats.
- [x] Task: Verify that `RestDocs` correctly document the new standardized response fields.
- [x] Task: Conductor - User Manual Verification 'Phase 3: Adoption and Validation' (Protocol in workflow.md)

## Phase 4: Final Cleanup and Documentation [x]
Ensuring consistency and final polish.

- [x] Task: Perform final code review for consistency with project style guides.
- [x] Task: Document the new API response and error handling protocol in the project documentation (e.g., `GUIDE.md` or a new API-standard.md).
- [x] Task: Conductor - User Manual Verification 'Phase 4: Final Cleanup and Documentation' (Protocol in workflow.md)
