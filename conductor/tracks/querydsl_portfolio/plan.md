# Implementation Plan: Portfolio Domain QueryDSL Implementation

## Phase 1: Portfolio Custom Repository Setup
- [x] Task: Create `PortfolioCustomRepository` and `PortfolioRepositoryImpl`
- [x] Task: Migrate `findWithItems` to QueryDSL with Fetch Join
- [x] Task: Verify with Repository Test

## Phase 2: Query Optimization
- [x] Task: Optimize `loadAllPortfolios` to optionally include items
- [x] Task: Implement dynamic filtering (e.g., by name or item count)

## Phase 3: Integration & Cleanup
- [x] Task: Update `PortfolioAdapter`
- [x] Task: Remove old `@Query` methods
- [x] Task: Final verification
