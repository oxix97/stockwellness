# Specification: Portfolio Simulation & Admin (Track H - Phase 1)

## Overview
This track focuses on the major refactoring and enhancement of the Portfolio domain to support sophisticated simulation, asset management, and administrative monitoring. This is part of the "H. 포트폴리오 시뮬레이션 & 관리" feature group, specifically targeting core CRUD, performance tracking, and system health checks.

## Objectives
- Refactor the existing `Portfolio` and `PortfolioItem` domain models to improve extensibility and support simulation logic.
- Implement comprehensive Portfolio CRUD operations.
- Provide real-time and historical performance analysis (daily P/L, asset/sector/country diversification).
- Integrate administrative tools for batch monitoring and data integrity verification.

## Functional Requirements
### 1. Core Portfolio Management (81-82)
- Create, Read, Update, and Delete (CRUD) operations for user portfolios.
- Add/Remove assets (stocks, cash) with purchase price and quantity.
- Support multiple portfolios per user.

### 2. Performance & Diversification Analysis (83-87)
- **Daily P/L:** Calculate daily profit/loss dynamically based on the latest EOD prices.
- **Diversification Analysis:**
    - Sector weight analysis (Pie chart data).
    - Country weight analysis (KR/US split).
    - Cash vs. Asset ratio.
- **Dynamic Performance Calculation:** Implement on-the-fly calculation of cumulative returns and P/L history from historical price data.

### 3. Administrative Monitoring (95-100)
- **Batch Monitoring:** Dashboard or API to check the success of daily data collection batch jobs.
- **Manual Trigger:** Internal capability to re-trigger batch jobs for specific dates or markets.
- **Data Integrity:** Check for price gaps or missing data in the `StockPrice` table.
- **System Health:** Endpoint to verify connectivity to Database, Redis, and Kafka.

## Non-Functional Requirements
- **Performance:** Dynamic P/L calculation must be optimized using Redis-cached price data to ensure low latency.
- **Scalability:** Refactored domain logic should be compatible with Java 21 Virtual Threads for concurrent calculation.

## Acceptance Criteria
- [ ] Users can create and manage multiple portfolios.
- [ ] Sector and country distribution ratios are accurately calculated.
- [ ] Daily P/L matches the latest market price.
- [ ] Admin can monitor batch job statuses via API.
- [ ] Health check endpoint returns status for all core dependencies (DB, Redis, Kafka).

## Out of Scope (For this Phase)
- Advanced simulation (Rebalancing, DCA/Lump-sum backtesting).
- Risk metrics (MDD, Sharpe, Beta, Correlation).
- These will be addressed in subsequent phases.
