# Implementation Plan: Portfolio Simulation & Admin (Track H - Phase 1)

This plan outlines the major refactoring of the Portfolio domain and the implementation of core CRUD, performance analysis, and administrative monitoring features.

## Phase 1: Portfolio Domain Refactoring & Core CRUD
Focus on restructuring the domain model to support better extensibility and implementing basic management features.

- [ ] **Task: Domain Model Refactoring**
    - [ ] Refactor `Portfolio` and `PortfolioItem` to support flexible asset types (Stock, Cash).
    - [ ] Update `Portfolio` to include metadata for diversification analysis.
    - [ ] Implement domain-level validation for portfolio constraints (e.g., duplicate names).
- [ ] **Task: Portfolio CRUD Implementation**
    - [ ] Implement `PortfolioService` with create, read, update, and delete logic.
    - [ ] Implement `PortfolioRepository` (Spring Data JPA) with necessary query methods.
    - [ ] Create REST API endpoints for portfolio management in `stockwellness-api`.
- [ ] **Task: Asset Management Logic**
    - [ ] Implement logic to add/remove `PortfolioItem` (Stock) with purchase price and quantity.
    - [ ] Implement logic to manage `Cash` balance within a portfolio.
- [ ] **Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)**

## Phase 2: Performance & Diversification Analysis
Implement dynamic calculation of P/L and diversification metrics.

- [ ] **Task: Dynamic P/L Calculation Engine**
    - [ ] Implement a service to fetch the latest EOD prices from Redis/DB.
    - [ ] Create a calculation utility for real-time portfolio valuation and P/L.
    - [ ] Optimize performance using Redis-cached price data.
- [ ] **Task: Diversification Metrics**
    - [ ] Implement sector weight calculation logic.
    - [ ] Implement country weight (KR/US) calculation logic.
    - [ ] Implement cash vs. asset ratio calculation.
- [ ] **Task: API Integration for Analysis**
    - [ ] Create API endpoints to return portfolio summary and diversification data.
    - [ ] Ensure responses are compatible with chart requirements (Pie chart, Bar chart).
- [ ] **Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)**

## Phase 3: Administrative Monitoring & Health Check
Implement tools for system observability and batch management.

- [ ] **Task: System Health Check API**
    - [ ] Implement health check indicators for Database, Redis, and Kafka.
    - [ ] Create a unified health check endpoint `/api/admin/health`.
- [ ] **Task: Batch Monitoring & Integrity**
    - [ ] Implement an API to query the status of Spring Batch jobs from `BATCH_JOB_EXECUTION`.
    - [ ] Implement a data integrity check for `StockPrice` gaps.
- [ ] **Task: Admin Control Features**
    - [ ] (Optional) Implement a secure endpoint to manually trigger data sync for a specific date.
- [ ] **Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)**
