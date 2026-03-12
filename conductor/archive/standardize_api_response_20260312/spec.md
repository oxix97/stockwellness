# Track: Standardizing Common Response and Global Exception Handling (spec.md)

## Overview
This track aims to standardize the API response format across all services in the Stockwellness project. By implementing a consistent response structure and a global exception handling mechanism, we will improve the developer experience and provide a clear communication protocol for clients.

## Functional Requirements
- **Standardized Success Response (`ApiResponse<T>`):** All successful API calls MUST return a consistent JSON wrapper using Java 21 `record`.
- **Standardized Error Response (`ErrorResponse`):** All error cases MUST return a consistent JSON wrapper using Java 21 `record`, including a machine-readable code, message, and field-level validation errors where applicable.
- **Global Exception Handler:** Centralize exception handling in the `stockwellness-api` module to catch all exceptions and convert them into the `ErrorResponse` format.
- **Error Code Management:** Use an `ErrorCode` Enum to define business error codes, mapping them to HTTP status codes and default messages.
- **Trace ID Support:** Include a `traceId` or `requestId` in error responses to facilitate production debugging.
- **Validation Support:** Integrate with Spring Bean Validation (`@Valid`) to return detailed field-level errors in the `ErrorResponse`.

## Technical Specifications
- **Java 21 Records:** Use `record` for all response DTOs (`ApiResponse`, `ErrorResponse`, `FieldError`).
- **Spring Boot Support:** Leverage `@RestControllerAdvice` for global exception handling.
- **Location:**
  - `ApiResponse`, `ErrorResponse`, `ErrorCode`, and base exception classes should reside in `stockwellness-core` (package `org.stockwellness.global.common/error`).
  - `GlobalExceptionHandler` should reside in `stockwellness-api`.

## Acceptance Criteria
- [ ] All successful API responses follow the `ApiResponse<T>` structure.
- [ ] All error responses follow the `ErrorResponse` structure with correct HTTP status codes.
- [ ] Field-level validation errors are correctly captured and returned in `ErrorResponse`.
- [ ] `Trace ID` is present in error responses and matches the server logs.
- [ ] `GlobalExceptionHandler` uses Java 21 switch pattern matching for exception handling.

## Out of Scope
- Migrating every single existing controller to the new format (focus on core controllers like `PortfolioController` and `MemberController` as initial implementation).
- Advanced monitoring integration beyond basic logging of Trace IDs.
