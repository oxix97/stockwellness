# Findings - Phase 1

## AuthController.java
1. **Violation (Dependency Direction):** Depends directly on implementation `AuthService` instead of an Input Port interface.
    - `import org.stockwellness.application.service.AuthService;`
    - `private final AuthService authService;`
2. **Violation (Domain Leakage):** Depends directly on Domain Entity `Member`.
    - `import org.stockwellness.domain.member.Member;`
    - `@AuthenticationPrincipal Member member` in `logout` method.
\n## DTOs\n1. **Potential Violation (Domain Leakage):** `LoginRequest` imports `org.stockwellness.domain.member.LoginType`. Strict decoupling would suggest using a String or a separate Enum.
\n## Input Ports & Dependencies\n1. **Violation (Missing Input Ports):** `AuthController` calls `AuthService` (concrete class) directly. No Input Port interfaces exist.\n2. **CRITICAL Violation (Reverse Dependency):** `AuthService` (Application Layer) imports DTOs from `org.stockwellness.adapter.in.web.auth.dto` (Adapter Layer). The Application Core is depending on the Web Adapter.
\n## Phase 2: Detailed Violations\n1. **Violation (Method Signature):** `AuthController.logout` takes `Member` (Domain Entity) as an argument. This forces a direct dependency from Adapter to Domain.
2. **Violation (Missing Input Ports):** `AuthController` depends directly on the concrete class `AuthService`. No abstraction (Input Port) is used to decouple the Adapter from the Application Service.
3. **Compliance (No Business Logic):** `AuthController` methods are correctly thin and delegate all business operations to the application layer.
4. **Compliance (No Cross-Adapter Dependencies):** The auth web adapter does not depend on any other adapters.
