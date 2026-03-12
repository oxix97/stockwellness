# Implementation Plan: Global Pointcut Logging System

## [x] Phase 1: Infrastructure & Custom Annotation
- [x] Task: Define the `@LogExecution` custom annotation in the Core module.
    - [x] Create `com.stockwellness.core.common.logging.LogExecution` annotation.
    - [x] Add necessary Retention and Target policies.
- [x] Task: Create initial test cases for the annotation.
    - [x] Verify that the annotation is present on target classes/methods.
- [~] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## [x] Phase 2: AOP Aspect & Pointcut Implementation
- [x] Task: Implement the `LoggingAspect` class in the Core module.
    - [x] Create `com.stockwellness.core.common.logging.LoggingAspect`.
    - [x] Configure it as a Spring Component and use `@Aspect`.
- [x] Task: Define Pointcut expressions for Hybrid Filtering.
    - [x] Implement pointcuts for package-based filtering (API/Core/Batch application layers).
    - [x] Implement pointcut for methods marked with `@LogExecution`.
- [x] Task: Write tests for pointcut matching.
    - [x] Verify the Aspect intercepts methods within target packages.
    - [x] Verify the Aspect intercepts methods annotated with `@LogExecution`.
- [~] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## [x] Phase 3: Logging Logic & JSON Formatting
- [x] Task: Implement the `@Around` advice for method boundaries and execution time.
    - [x] Logic to capture start time and calculate duration.
    - [x] Logic to capture method name and class.
- [x] Task: Implement Argument, Return Value, and Exception capture.
    - [x] Securely extract method arguments.
    - [x] Capture the return value or the thrown exception.
- [x] Task: Implement Structured JSON Formatting.
    - [x] Use Jackson or SLF4J structured logging to output logs in JSON format.
    - [x] Ensure logs include timestamp, level, logger, method details, and context.
- [x] Task: Write tests for logging content and JSON format.
    - [x] Verify that arguments and return values are correctly captured in the log.
    - [x] Verify that exceptions are logged with the stack trace.
- [~] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## [x] Phase 4: Integration & Final Validation
- [x] Task: Enable Aspect across all modules and verify end-to-end.
    - [x] Ensure the AOP configuration is picked up by API and Batch modules.
    - [x] Check logs during a typical API request and a Batch job execution.
- [x] Task: Final Quality Audit.
    - [x] Check code coverage for the logging system (>80%).
    - [x] Review performance impact and ensure no sensitive data is leaked in logs.
- [x] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)