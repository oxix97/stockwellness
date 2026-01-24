# Product Guidelines

## 1. Content & Prose Style
- **Professional & Analytical:** Reports and user-facing content must be objective, data-heavy, and precise to maintain financial credibility.
- **Accessible & Educational:** While maintaining precision, use accessible language and explain technical terms to empower beginners and prevent alienation.

## 2. Brand Values & Messaging
- **Data-Driven Objectivity:** Prioritize empirical evidence and pre-calculated technical indicators over market sentiment or hype. Build trust through transparency in data analysis.
- **User Empowerment:** Focus on providing users with the tools and knowledge to execute disciplined strategies, moving from impulse to intent.
- **Simplicity in Complexity:** Strive to make sophisticated financial simulations and engineering architectures (like Hexagonal and Virtual Threads) feel intuitive and manageable for the end user.

## 3. Visual Identity & UX Principles
- **Clean & Minimalist:** Prioritize clarity and readability. Use whitespace effectively to prevent information overload, especially when presenting complex data.
- **Trustworthy & Stable:** Employ a color palette (e.g., deep blues, professional greens, and balanced neutrals) that conveys stability, reliability, and professional financial management.

## 4. Engineering & Operational Principles
- **Pragmatic Hexagonal Architecture:**
    - Separate business logic from technical details in principle, but allow for a **Hybrid Model** where Domain Models and JPA Entities are integrated for development efficiency.
    - **Constraint:** Even in this hybrid approach, ensure domain logic remains unpolluted by external libraries. Strictly maintain the structure of external communication via Service interfaces and Ports.
- **Modular & Test-Driven Development (TDD):**
    - Verify core business rules via Unit Tests.
    - In the entity-integrated structure, utilize `DataJpaTest` to test the persistence logic of the repository layer together.
- **Observability-First:**
    - Monitor actual DB queries via **P6Spy**.
    - Log detailed execution steps for Kafka event publishing and Batch processing stages.
- **Reliability over Speed:**
    - Accuracy of financial data is paramount.
    - Must include logic to verify batch data consistency.
    - Design **Retry mechanisms** to handle scenarios of data corruption or processing failure.
