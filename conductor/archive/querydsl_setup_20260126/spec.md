# Specification: QueryDSL Configuration

## 1. Overview
Configure QueryDSL 5.x/6.x (Jakarta) in the project to enable type-safe dynamic queries for the persistence layer. This track focuses purely on infrastructure setup and verification, establishing the foundation for future custom repository implementations.

## 2. Goals
- **Dependency Management:** Add necessary QueryDSL dependencies and annotation processor configurations to `build.gradle.kts` compatible with Spring Boot 3.4.x and Java 21.
- **Configuration:** Implement a `QueryDslConfig` class to register the `JPAQueryFactory` bean.
- **Verification:** Ensure `Q` classes (Q-types) are generated correctly during the build process and verify the setup with a simple integration test.

## 3. Scope
### In Scope
- Modifying `build.gradle.kts` for QueryDSL.
- Creating `org.stockwellness.config.QueryDslConfig`.
- A basic test case (`QueryDslTest`) to verify the bean injection and Q-class generation.

### Out of Scope
- Refactoring existing repositories to use QueryDSL.
- Implementing complex dynamic queries for business logic (deferred to future tracks).

## 4. Technical Requirements
- **Version:** QueryDSL 5.x or 6.x (compatible with Jakarta Persistence/Hibernate 6).
- **Build System:** Gradle Kotlin DSL.
- **Output:** Generated Q-classes should be located in `build/generated/querydsl` (or standard Gradle build path) and recognized by the IDE.
