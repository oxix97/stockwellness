# Specification: Portfolio Domain QueryDSL Implementation

## Goal
Optimize portfolio data access using QueryDSL to handle complex joins (Fetch Joins) and dynamic filtering, reducing N+1 problems and improving code maintainability.

## Context
- `Portfolio` has a One-to-Many relationship with `PortfolioItem`.
- `PortfolioRepository` uses `@Query` with `JOIN FETCH`, which is standard but less flexible for dynamic conditions.
- Future requirements might include filtering portfolios by asset composition or performance.

## Requirements
- Migrate `findWithItems` to QueryDSL.
- Implement flexible portfolio retrieval for members.
- Ensure efficient loading of items to avoid N+1.
