# Specification: Global Pointcut Logging System

## Overview
Implement a centralized, global logging system using Spring AOP (Aspect-Oriented Programming) to capture standardized logs across all modules (API, Batch, Core) in the Stockwellness project. This system will provide high visibility into method execution, performance, and data flow while maintaining clean domain logic by separating logging concerns.

## Functional Requirements
- **Global AOP Aspect:** Create an AOP aspect that intercepts method executions in all major modules.
- **Hybrid Filtering:**
  - **Package-based:** Automatically log methods within specified base packages (e.g., `com.stockwellness.*.application.service`, `com.stockwellness.api.adapter.in.web`, etc.).
  - **Annotation-based:** Provide a custom annotation (e.g., `@LogExecution`) to explicitly trigger or customize logging for specific methods outside the standard package structure.
- **Log Content Capture:**
  - **Method Boundaries:** Log entry ("Entering method X") and exit ("Exiting method X") points.
  - **Execution Time:** Calculate and log the total duration of the method execution in milliseconds.
  - **Arguments & Return Values:** Capture method input parameters and the final return value for traceability.
  - **Exception Handling:** Automatically log exceptions, including the stack trace and relevant method context, when a failure occurs.
- **Structured JSON Output:** Ensure logs are formatted as structured JSON to facilitate ingestion and analysis by log aggregation tools (e.g., ELK Stack, CloudWatch).

## Non-Functional Requirements
- **Performance:** Minimize overhead by using efficient AOP pointcut expressions and ensuring heavy-weight operations (like large object serialization in arguments) are handled safely.
- **Maintainability:** Centralize logging configuration to allow easy adjustments to log levels and filtering rules.
- **Observability:** Improve system-wide observability, especially for long-running batch jobs and complex portfolio simulations.

## Acceptance Criteria
- [ ] A custom annotation (e.g., `@LogExecution`) is defined and functional.
- [ ] An AOP Aspect is implemented that intercepts methods in API, Batch, and Core modules.
- [ ] Logs are produced in a structured JSON format.
- [ ] Logs include: method name, execution time, arguments, return values, and exceptions.
- [ ] The logging system correctly filters methods based on both package patterns and annotations.
- [ ] Unit tests verify the Aspect's pointcut matching and log generation logic.

## Out of Scope
- Integration with a specific external log aggregator (e.g., setting up an ELK cluster).
- Logging of sensitive PII (Personally Identifiable Information) like passwords or tokens (should be masked or excluded).