# Specification: Stock Domain QueryDSL Implementation

## Goal
Improve code maintainability, type safety, and query performance in the Stock domain by replacing string-based JPQL and rigid method-name queries with QueryDSL.

## Context
- `StockRepository` uses `@Query` for dynamic search, which is error-prone.
- `StockHistoryRepository` has many hardcoded `findTopN` methods.
- `StockHistory` uses a composite key (`StockHistoryId`).

## Requirements
- Migrate `StockRepository.searchByCondition` to QueryDSL.
- Use `BooleanExpression` for reusable search conditions.
- Create flexible history retrieval methods in `StockHistoryRepository` using `.limit()`.
- Ensure integration with existing `StockAdapter`.
