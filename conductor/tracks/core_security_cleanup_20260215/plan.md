# Implementation Plan: Core Security Cleanup

This plan follows a step-by-step refactoring approach to remove security library dependencies from the `core` module.

## Phase 1: Domain & Service Layer Decoupling
- [x] Task: Update Service Method Signatures
    - [x] Change `MemberPrincipal` parameters to `Long memberId` in all UseCases and Services within `core`.
    - [x] Update Controllers in `api` to extract `id` from `@AuthenticationPrincipal MemberPrincipal` before calling services.
- [x] Task: Clean up imports
    - [x] Remove all `org.stockwellness.global.security` imports from `core` services.

## Phase 2: Component Relocation (Core -> API)
- [x] Task: Move Security Classes
    - [x] Move `MemberPrincipal.java` to `stockwellness-api`.
    - [x] Move `JwtProvider.java`, `JwtProperties.java`, `GenerateTokenPort.java`, `ValidateTokenPort.java` to `stockwellness-api`.
    - [x] Move `CustomOAuth2UserService.java`, `CustomUserDetailsService.java` to `stockwellness-api`.
- [x] Task: Evaluation of `RefreshTokenPort`
    - [x] Evaluate if `RefreshTokenPort` (Redis implementation) should stay in `core` or move to `api`. (Move to API confirmed)

## Phase 3: Dependency Cleanup
- [x] Task: Refactor `core/build.gradle.kts`
    - [x] Remove `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, and `jjwt` dependencies.
- [x] Task: Refactor `api/build.gradle.kts`
    - [x] Add the removed dependencies directly to the `api` module.

## Phase 4: Verification & Finalization
- [x] Task: Fix Tests
    - [x] Update `core` unit tests to use `Long memberId` instead of mocking `MemberPrincipal`.
    - [x] Ensure `api` integration tests work with relocated security classes.
- [x] Task: Final Build & Run
    - [x] Execute `./gradlew clean build`.
    - [x] Verify that Batch module does not have security dependencies.
