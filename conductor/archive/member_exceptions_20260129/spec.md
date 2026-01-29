# Specification: Member Custom Exception Implementation

## 1. Overview
Refactor the exception handling in the Member domain to follow a hierarchical structure. Instead of using a generic `MemberDomainException`, we will implement specific exception classes (`MemberNotFoundException`, `EmailDuplicateException`, `NicknameDuplicateException`) that extend the global `BusinessException`. This ensures that domain errors are explicitly defined and automatically mapped to the correct `ErrorCode` and HTTP status by the global exception handler.

## 2. Functional Requirements
- **Create Custom Exceptions:**
    - Create `MemberNotFoundException` in `org.stockwellness.domain.member.exception`.
    - Create `EmailDuplicateException` in `org.stockwellness.domain.member.exception`.
    - Create `NicknameDuplicateException` in `org.stockwellness.domain.member.exception`.
- **Inheritance:** All new exceptions must extend `org.stockwellness.global.error.exception.BusinessException`.
- **ErrorCode Mapping:** Each exception must pass the specific `ErrorCode` to the parent constructor:
    - `MemberNotFoundException` -> `ErrorCode.MEMBER_NOT_FOUND`
    - `EmailDuplicateException` -> `ErrorCode.DUPLICATE_EMAIL`
    - `NicknameDuplicateException` -> `ErrorCode.DUPLICATE_NICKNAME`
- **Refactor Usage:**
    - Update `Member` entity and related services to throw these specific exceptions instead of `MemberDomainException` (or `IllegalArgumentException`) where applicable.

## 3. Non-Functional Requirements
- **Maintainability:** Code should be self-explanatory. `throw new MemberNotFoundException()` is preferred over generic exceptions.
- **Consistency:** Follow the existing `BusinessException` pattern used in the project.

## 4. Acceptance Criteria
- [ ] `MemberNotFoundException`, `EmailDuplicateException`, and `NicknameDuplicateException` classes exist.
- [ ] All new exceptions extend `BusinessException`.
- [ ] `Member` entity and Service layer code throws specific exceptions for the defined error cases.
- [ ] `GlobalExceptionHandler` correctly returns the specific HTTP status and error message defined in `ErrorCode` when these exceptions are thrown (verified via test or code review).
- [ ] (Optional) Remove `MemberDomainException` if it is no longer used, or deprecate it.

## 5. Out of Scope
- Modifying exceptions for other domains (e.g., Portfolio).
- Changing the `GlobalExceptionHandler` logic itself (it already supports `BusinessException`).
