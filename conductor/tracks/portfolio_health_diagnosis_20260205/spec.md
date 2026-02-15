# Portfolio Health Diagnosis Enhancement

## 1. Overview
This track implements a "Portfolio Health Diagnosis" system that evaluates a user's portfolio across five key dimensions. It provides a gamified, data-driven assessment to help investors understand their portfolio's characteristics and receive actionable advice.

## 2. Functional Requirements

### 2.1 Diagnosis Dimensions & Scoring Logic
- **Defense (Defense) - Market Cap:**
    - 100 pts: Market Cap ≥ 20T
    - 80 pts: 5T ≤ Market Cap < 20T
    - 60 pts: 1T ≤ Market Cap < 5T
    - 40 pts: Market Cap < 1T
- **Attack (Attack) - RSI14 & MACD:**
    - Formula: `(RSI Score + MACD Bonus) / 2`
    - RSI: 70+ (90 pts), 50-70 (70 pts), <50 (40 pts)
    - MACD: ≥ 0 (+10 bonus pts)
- **Endurance (Endurance) - MA120 Disparity:**
    - 100 pts: 100% ≤ Disparity ≤ 110% (Price / MA120)
    - 70 pts: Disparity > 110%
    - 40 pts: Disparity < 100%
- **Agility (Agility) - Volatility:**
    - Based on 5-day absolute average fluctuation rate.
    - 100 pts: Avg > 5%
    - 70 pts: 2% - 5%
    - 40 pts: < 2%
- **Balance (Balance) - Diversification (No Sector Data):**
    - Formula: `(Stock Count Score + Market Dispersion Score) / 2`
    - Stock Count Score: 1 (20 pts), 2-3 (60 pts), 4+ (100 pts)
    - Market Dispersion Score: Mixing KOSPI and KOSDAQ stocks (100 pts), Single market only (50 pts)

### 2.2 Aggregation Logic
- Portfolio health is calculated by weighting individual stock stats by their `pieceCount`.
- **Formula:** `Category Score = Σ(Stock_Stat_i * pieceCount_i) / 8` (Divided by MAX_PIECES)
- The "Total Score" is the simple average of the five category scores.

### 2.3 Data Handling
- **Missing Data:** If `StockPrice` indicators are null (e.g., new listing), use a default score of 50 for that specific dimension.
- **Precision:** Handle `marketCap` (BigDecimal) and other numeric calculations with appropriate precision (scale: 2 or higher).

### 2.4 API Interface
- **Endpoint:** `GET /api/v1/portfolios/{portfolioId}/health`
- **Response Structure (JSON):**
    - `overallScore`: Integer (0-100)
    - `categories`: Map of scores (attack, defense, endurance, agility, balance)
    - `stockContributions`: List of objects containing `name`, `mainContribution`, `score`, and `reason`.
    - `aiDiagnosis`: `summary` (e.g., "Growth Archer") and `insight`.
    - `nextSteps`: List of actionable strings (e.g., "Add 1 piece of KOSPI large-cap for defense").

## 3. Technical Implementation Details
- **Architecture:** Hexagonal Architecture (Domain-Centric).
- **Service Layer:**
    - `StockStatService`: Calculates dimensions for a single stock using `StockPrice`.
    - `PortfolioDiagnosisService`: Orchestrates the overall calculation using Batch Fetching for all stock histories in the portfolio.
- **Infrastructure:**
    - Ensure `StockPrice` is indexed on `isinCode` and `baseDate` for performance.
- **AI Integration:** Use Spring AI to generate `summary`, `insight`, and `nextSteps` based on the final calculated scores and weights.

## 4. Acceptance Criteria
- [ ] API successfully returns the diagnosis JSON for a given portfolio within 1 second.
- [ ] Scoring logic matches the thresholds defined in section 2.1.
- [ ] Portfolio weights (pieceCount) are correctly applied (Total weight = 8).
- [ ] AI-generated insights are relevant to the calculated scores and follow a "Beginner-friendly" tone.
- [ ] Graceful handling of missing `StockPrice` data (defaulting to 50).
