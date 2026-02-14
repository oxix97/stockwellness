# Specification: Stock Domain QueryDSL Implementation

## Goal
Improve code maintainability, type safety, and query performance in the Stock domain by replacing string-based JPQL and rigid method-name queries with QueryDSL.

## Context
- `StockRepository` uses `@Query` for dynamic search, which is error-prone.
- `StockPriceRepository` has many hardcoded `findTopN` methods.
- `StockPrice` uses a composite key (`StockPriceId`).

## Requirements
- Migrate `StockRepository.searchByCondition` to QueryDSL.
- Use `BooleanExpression` for reusable search conditions.
- Create flexible stockPrice retrieval methods in `StockPriceRepository` using `.limit()`.
- Ensure integration with existing `StockAdapter`.
