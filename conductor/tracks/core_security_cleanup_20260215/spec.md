# Specification: Core Security Cleanup Refactoring

## 1. Overview
The current `stockwellness-core` module is intended to contain only pure domain logic and common infrastructure interfaces. However, it currently depends on Spring Security and JWT libraries, which are "Web/Adapter" concerns. This track will remove these dependencies from `core` and move them to the `stockwellness-api` module.

## 2. Goals
- **Domain Purity:** Ensure `stockwellness-core` has zero dependencies on `spring-boot-starter-security` and JWT libraries.
- **Improved Cohesion:** Centralize all authentication and authorization logic within the `stockwellness-api` module.
- **Lighter Batch:** Enable `stockwellness-batch` to run without loading security contexts or related beans.

## 3. Technical Requirements
- **Decoupling Service Layer:** Service methods in `core` must not take `MemberPrincipal` as a parameter. They should use primitive types or domain-specific DTOs (e.g., `Long memberId`).
- **Component Relocation:**
    - `MemberPrincipal`, `JwtProvider`, `JwtProperties`, `CustomOAuth2UserService`, `CustomUserDetailsService` must move to the `api` module.
- **Port/Adapter Cleanup:** 
    - `GenerateTokenPort` and `ValidateTokenPort` should be evaluated. If only used by `api` for auth flow, they should move to `api` or be removed if the implementation can be used directly within the `api` boundary.
- **Build Configuration:**
    - `core/build.gradle.kts`: Remove security and JWT dependencies.
    - `api/build.gradle.kts`: Add required security and JWT dependencies.

## 4. Acceptance Criteria
- [ ] `stockwellness-core` successfully builds without security/JWT dependencies.
- [ ] `stockwellness-batch` no longer has security-related classes on its classpath.
- [ ] `stockwellness-api` continues to support OAuth2 login and JWT authentication.
- [ ] All existing tests pass after the refactoring.
- [ ] No circular dependencies between modules.
