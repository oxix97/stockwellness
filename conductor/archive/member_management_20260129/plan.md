# Implementation Plan - Member Management Review

## Phase 1: User Implementation (Self-Guided)
- [x] Task: Implement Domain Logic
    - [x] Add `update()` and `deactivate()` methods to `Member` entity.
    - [x] Implement validation logic (e.g., nickname rules).
- [x] Task: Implement Application Layer
    - [x] Create `MemberUsecase` (Input Port) and `MemberService`.
    - [x] Implement `getMember`, `updateMember`, `withdrawMember`.
    - [x] Define `LoadMemberPort` and `SaveMemberPort` (if needed).
- [x] Task: Implement Adapters
    - [x] `MemberController`: Implement API endpoints.
    - [x] `MemberPersistenceAdapter`: Implement query/update methods.
- [x] Task: Conductor - User Manual Verification 'User Implementation' (Protocol in workflow.md)

## Phase 2: Code Review & Quality Assurance
- [x] Task: Architectural Review
    - [x] Check dependency direction (Adapter -> Application -> Domain).
    - [x] Verify DTO usage (ensure Domain entities don't leak to Controller).
- [x] Task: Business Logic & Defect Detection
    - [x] Review Nickname Validation Logic (Null, Empty, Length, Duplication).
    - [x] Review Soft Delete Logic (Impact on login/auth).
    - [x] Check Transaction Management (`@Transactional`).
- [x] Task: Test Coverage Review
    - [x] Verify Unit Tests for `Member` entity.
    - [x] Verify Slice Tests for `MemberController`.
    - [x] Suggest additional test cases for edge scenarios.
- [x] Task: Conductor - User Manual Verification 'Code Review & Quality Assurance' (Protocol in workflow.md)

## Phase 3: Final Polish
- [x] Task: Refactoring (Optional)
    - [x] Apply improvements suggested during the review phase (added MemberDomainException, fixed isActive checks).
- [x] Task: Documentation Update
    - [x] Update API Documentation (Swagger/RestDocs).
- [x] Task: Conductor - User Manual Verification 'Final Polish' (Protocol in workflow.md)
