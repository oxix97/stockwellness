# Hexagonal Architecture Review Report: Auth Web Adapter

## 1. Executive Summary
This report evaluates the `org.stockwellness.adapter.in.web.auth` package for compliance with Hexagonal Architecture principles. While the adapter correctly maintains thin controllers and avoids cross-adapter dependencies, several significant violations were identified, most notably a **Reverse Dependency** where the Application Layer depends on the Adapter Layer (DTOs).

## 2. Findings & Violations

### 2.1. Dependency Direction Violations

#### [CRITICAL] Reverse Dependency (Service -> Adapter DTOs)
*   **Issue:** The `AuthService` (Application Layer) directly imports and uses DTOs defined in the `adapter.in.web` package.
*   **Evidence (`AuthService.java`):**
    ```java
    import org.stockwellness.adapter.in.web.auth.dto.LoginRequest;
    import org.stockwellness.adapter.in.web.auth.dto.LoginResponse;
    import org.stockwellness.adapter.in.web.auth.dto.ReissueResponse;
    ```
*   **Impact:** This creates a dependency cycle and violates the core principle that the Application Layer must be independent of the Adapter Layer. Changes to the Web API contract (DTOs) could force changes in the Application Service.

#### [HIGH] Direct Dependency on Domain Entities
*   **Issue:** `AuthController` directly imports and uses the `Member` domain entity in method signatures.
*   **Evidence (`AuthController.java`):**
    ```java
    import org.stockwellness.domain.member.Member;
    ...
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Member member) {
        authService.logout(member.getId());
        return ResponseEntity.ok().build();
    }
    ```
*   **Impact:** Couples the Web Adapter layer directly to the Domain layer, bypassing the Application Layer's role as a boundary.

### 2.2. Port Usage Violations

#### [MEDIUM] Missing Input Ports (UseCases)
*   **Issue:** `AuthController` depends on a concrete Service class (`AuthService`) instead of an Input Port interface (e.g., `LoginUseCase`).
*   **Evidence (`AuthController.java`):**
    ```java
    private final AuthService authService;
    ```
*   **Impact:** Limits the ability to swap implementations or mock the application core without relying on the concrete service implementation.

### 2.3. DTO Isolation Violations

#### [LOW] Domain Enum Leakage in DTOs
*   **Issue:** `LoginRequest` uses a Domain Enum (`LoginType`).
*   **Evidence (`LoginRequest.java`):**
    ```java
    import org.stockwellness.domain.member.LoginType;
    ```
*   **Impact:** Minor coupling between the API contract and Domain definitions.

## 3. Compliance & Positive Observations
*   **Thin Controllers:** `AuthController` does not contain any business logic; it purely delegates to the application layer.
*   **Adapter Isolation:** No dependencies on other adapters (e.g., persistence or other web adapters) were found.

## 4. Recommendations

1.  **Resolve Reverse Dependency:**
    *   Move the logic-heavy DTOs (or create separate "Command" objects) into the Application Layer (`application.port.in`).
    *   The Controller should map Web-specific DTOs to these Application-layer Commands.
2.  **Introduce Input Ports:**
    *   Define interfaces like `LoginUseCase`, `ReissueUseCase`, and `LogoutUseCase` in `application.port.in`.
    *   Make `AuthService` implement these interfaces.
    *   Update `AuthController` to depend on these interfaces.
3.  **Isolate Domain Principal:**
    *   Modify `logout` to accept a primitive ID or a DTO instead of the `Member` entity.
    *   Use a custom ArgumentResolver or map the Principal to an ID within the Controller.
4.  **Decouple Enums:**
    *   Consider using a String or a dedicated DTO-layer Enum for `LoginType` to fully decouple the API from Domain changes.
