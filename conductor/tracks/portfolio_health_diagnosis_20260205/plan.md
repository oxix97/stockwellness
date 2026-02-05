# Implementation Plan: Portfolio Health Diagnosis Enhancement

This plan outlines the steps to implement the Portfolio Health Diagnosis system, following the Hexagonal Architecture and TDD principles defined in the project workflow.

## Phase 1: Domain & Stock Scoring Logic
Focus on defining the core domain models and the logic for scoring individual stocks.

- [x] Task: Define Domain Models and DTOs for Diagnosis
    - [x] Create `StockStat` domain model to hold 5-dimension scores for a stock.
    - [x] Create `PortfolioHealth` domain model for aggregated results.
    - [x] Create `DiagnosisResponse` DTO for API output.
- [x] Task: Implement `StockStatService` (TDD)
    - [x] Write tests for Defense score logic (Market Cap thresholds).
    - [x] Implement Defense scoring logic.
    - [x] Write tests for Attack score logic (RSI & MACD).
    - [x] Implement Attack scoring logic.
    - [x] Write tests for Endurance score logic (MA120 Disparity).
    - [x] Implement Endurance scoring logic.
    - [x] Write tests for Agility score logic (5-day Volatility).
    - [x] Implement Agility scoring logic.
    - [x] Handle null/missing data by defaulting to 50 pts in tests and implementation.
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Portfolio Aggregation & Service Layer
Implement the logic to aggregate individual stock scores into a portfolio-wide diagnosis.

- [x] Task: Implement `PortfolioDiagnosisService` Aggregation Logic (TDD)
    - [x] Write tests for weighting logic using `pieceCount` (total weight = 8).
    - [x] Implement weighted average calculation for each category.
    - [x] Implement Balance score logic (Stock count + Market dispersion).
    - [x] Optimize data fetching: Implement batch fetching for `StockHistory` of all ISIN codes in the portfolio.
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: AI Insight Integration
Integrate Spring AI to provide human-readable summaries and actionable advice.

- [ ] Task: Implement `AiDiagnosisService`
    - [ ] Define prompt templates for "Growth Archer" style summaries and beginner-friendly insights.
    - [ ] Integrate Spring AI to generate `summary`, `insight`, and `nextSteps` based on calculated scores.
    - [ ] Write integration tests for AI prompt generation (mocking AI response if necessary).
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Web Interface & Documentation
Expose the diagnosis functionality via a REST API and document it.

- [ ] Task: Implement `PortfolioHealthController`
    - [ ] Create `GET /api/v1/portfolios/{portfolioId}/health` endpoint.
    - [ ] Connect controller to `PortfolioDiagnosisService`.
- [ ] Task: API Documentation and Integration Testing
    - [ ] Write integration tests for the full diagnosis flow.
    - [ ] Generate Spring REST Docs for the new endpoint.
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)
