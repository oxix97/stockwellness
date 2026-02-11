# Track Specification: Stock Watchlist (Updated)

## 1. Overview
This track implements the **Stock Watchlist** feature, allowing users to organize and monitor their interested stocks across different markets (KRX, US). It integrates the project's core "Wellness" identity by displaying pre-calculated technical indicators and AI insights.

## 2. Functional Requirements

### 2.1 Watchlist Group Management (Aggregate Root)
- **Create Group:** Users can create custom watchlist groups.
- **Edit Group:** Rename existing groups.
- **Delete Group:** Soft-delete groups.
- **Default Group:** A "Default" group is automatically created when a new user signs up (triggered via `MemberCreatedEvent`).

### 2.2 Watchlist Item Management
- **Add Stock:** Users can add stocks to a specific group (supports KRX and US).
- **Duplicate Check:** **CRITICAL:** Prevent adding the same stock multiple times to the same group.
- **Remove Stock:** Users can remove stocks from a group (Soft Delete).
- **Constraints:**
    - Enforce maximum limits (e.g., 10 groups per user, 50 stocks per group).

### 2.3 Watchlist Viewing
- **List View:** Display stocks with EOD Price, Change Rate, and Wellness Status (RSI, etc.).
- **Market Context:** Explicitly show data timestamps or market status (Open/Closed).
- **AI Insight:** Toggle/view pre-calculated one-line AI analysis.

## 3. Technical Implementation

### 3.1 Architecture & Design
- **Domain Model:** `WatchlistGroup` (Aggregate Root) and `WatchlistItem`.
- **Persistence:** Use **QueryDSL** for queries and implement **Soft Delete** (`deleted_at`).
- **Integration:** Event-driven initialization via `MemberCreatedEvent`.

### 3.2 Performance & Caching
- **Redis Read-Through:** Check Redis first for indicators/prices.
- **Cache Synchronization:** **CRITICAL:** The Redis cache must be invalidated or updated immediately after the Daily Batch Process completes to ensure users see the latest EOD indicators.

### 3.3 Security
- **Ownership Verification:** AOP or Security Expressions to ensure users only access their own groups.

## 4. Acceptance Criteria
- [ ] New user gets a "Default" group automatically.
- [ ] User can manage groups (CRUD) and add/remove stocks.
- [ ] **System prevents duplicate stocks in the same group.**
- [ ] List view shows Wellness indicators and AI insights.
- [ ] **Redis cache is synchronized with the latest batch results.**
- [ ] Security checks prevent cross-user access.

## 5. Out of Scope
- Real-time stock price updates (WebSocket/Streaming).
- On-the-fly indicator calculation, and historical charts.
