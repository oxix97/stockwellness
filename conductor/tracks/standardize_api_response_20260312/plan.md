# Implementation Plan: Standardizing API Response and Global Exception Handling (plan.md)

## Phase 1: Core Foundation (stockwellness-core)
Building the foundational components in the core module for global use.

- [ ] Task: Create `ApiResponse<T>` record in `org.stockwellness.global.common`.
- [ ] Task: Create `ErrorCode` Enum with fields for HTTP Status, custom code, and default message.
- [ ] Task: Create `ErrorResponse` record in `org.stockwellness.global.error` including a list of `FieldError` records.
- [ ] Task: Implement `BusinessException` as an abstract class and migrate common domain exceptions to use it.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Core Foundation' (Protocol in workflow.md)

## Phase 2: Global Handler Enhancement (stockwellness-api)
Implementing the logic to catch exceptions and return standardized responses.

- [ ] Task: Implement `GlobalExceptionHandler` using `@RestControllerAdvice`.
- [ ] Task: Use Java 21 `switch pattern matching` in `GlobalExceptionHandler` to handle different exception types.
- [ ] Task: Implement logic to handle `MethodArgumentNotValidException` and populate `FieldError` details.
- [ ] Task: Integrate `Trace ID` (MDC or request header) into the `ErrorResponse` generation.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Global Handler Enhancement' (Protocol in workflow.md)

## Phase 3: Adoption and Validation (Integration)
Updating existing controllers and verifying the new structure.

- [ ] Task: Update `PortfolioController` and `MemberController` to use `ApiResponse<T>` as the return type.
- [ ] Task: Create integration tests to verify both success and error response formats.
- [ ] Task: Verify that `RestDocs` correctly document the new standardized response fields.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Adoption and Validation' (Protocol in workflow.md)

## Phase 4: Final Cleanup and Documentation
Ensuring consistency and final polish.

- [ ] Task: Perform final code review for consistency with project style guides.
- [ ] Task: Document the new API response and error handling protocol in the project documentation (e.g., `GUIDE.md` or a new API-standard.md).
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Final Cleanup and Documentation' (Protocol in workflow.md)
