# Specification: Hexagonal Architecture Review (Auth Web Adapter)

## 1. Overview
Perform a code review of the `org.stockwellness.adapter.in.web.auth` package to evaluate its compliance with Hexagonal Architecture principles. This audit will focus on strict architectural boundaries, ensuring the web adapter functions purely as an interface to the application core without leaking logic or violating dependency rules.

## 2. Goals & Scope
The review will assess the following key areas:
*   **Dependency Direction:** Verify that the `adapter` package depends *only* on `application` ports and does not have unauthorized dependencies on `domain` entities or other adapters.
*   **Port Usage:** Ensure `AuthController` interacts with the application layer *exclusively* through defined Input Ports (interfaces).
*   **DTO Isolation:** Confirm that Domain Entities are not exposed directly in API requests or responses; DTOs (Data Transfer Objects) must be used.
*   **Business Logic Leakage:** Identify any business rules or domain logic incorrectly implemented within the Controller/Adapter layer.

## 3. Deliverables
*   A findings report (Markdown format) detailing:
    *   Violations found (if any).
    *   Code snippets illustrating issues.
    *   Recommendations for remediation.

## 4. Out of Scope
*   Refactoring code to fix identified issues (this will be handled in a separate follow-up track if necessary).
*   Reviewing other adapters or packages outside of `org.stockwellness.adapter.in.web.auth`.
