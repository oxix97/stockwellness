# Implementation Plan: Member Custom Exception Implementation

## Phase 1: Preparation & Setup
- [x] Task: Review current exception usage in Member domain
    - [x] Identify all locations where `MemberDomainException` is thrown
    - [x] Identify where `IllegalArgumentException` or other generic exceptions are used for member-related errors
- [x] Task: Conductor - User Manual Verification 'Preparation & Setup' (Protocol in workflow.md)

## Phase 2: Implement Custom Exceptions (TDD)
- [x] Task: Create specific exception classes
    - [x] Write tests to verify `MemberNotFoundException` extends `BusinessException` and maps to `MEMBER_NOT_FOUND`
    - [x] Implement `MemberNotFoundException`
    - [x] Write tests to verify `EmailDuplicateException` extends `BusinessException` and maps to `DUPLICATE_EMAIL`
    - [x] Implement `EmailDuplicateException`
    - [x] Write tests to verify `NicknameDuplicateException` extends `BusinessException` and maps to `DUPLICATE_NICKNAME`
    - [x] Implement `NicknameDuplicateException`
- [x] Task: Conductor - User Manual Verification 'Implement Custom Exceptions' (Protocol in workflow.md)

## Phase 3: Refactor Member Domain & Services
- [x] Task: Refactor `Member` entity
    - [x] Update `Member.validateNickname` to throw `NicknameDuplicateException` (if applicable) or update existing domain logic tests
    - [x] Ensure all domain-level validations throw the new specific exceptions
- [x] Task: Refactor `MemberService` (or relevant application services)
    - [x] Update member lookup logic to throw `MemberNotFoundException`
    - [x] Update registration/update logic to throw `EmailDuplicateException` or `NicknameDuplicateException`
- [x] Task: Verify `GlobalExceptionHandler` integration
    - [x] Write an integration test (or use existing ones) to ensure that throwing `MemberNotFoundException` results in the correct `ErrorCode` JSON response and HTTP status.
- [x] Task: Conductor - User Manual Verification 'Refactor Member Domain & Services' (Protocol in workflow.md)

## Phase 4: Cleanup & Finalization
- [x] Task: Remove or deprecate `MemberDomainException`
    - [x] Check if `MemberDomainException` is used anywhere else in the project
    - [x] If unused, delete the class
- [x] Task: Final Quality Gate Check
    - [x] Run all tests (`./gradlew test`)
    - [x] Verify code coverage for new/modified classes (>80%)
    - [x] Ensure no linting errors
- [x] Task: Conductor - User Manual Verification 'Cleanup & Finalization' (Protocol in workflow.md)
