# Initial Concept

Side-project

# Product Definition

## 1. Vision
Stockwellness is an asset allocation simulator and AI-powered prediction service designed to empower individual retail investors. It aims to replace emotional trading with data-driven decision-making by providing sophisticated simulation tools and objective AI insights based on technical analysis. The platform is built with engineering excellence at its core, ensuring high reliability, scalability, and maintainability.

## 2. Target Audience
- **Individual Retail Investors:** Users seeking disciplined, data-driven approaches to portfolio management and asset allocation.

## 3. Core Problems Solved
- **Complexity in Strategy:** Simplifies the implementation and backtesting of diverse asset allocation strategies.
- **Interpretation Barrier:** Lowers the barrier to understanding technical indicators and market trends without needing professional expertise.
- **Lack of Actionable Insights:** Provides accessible, AI-driven insights to predict stock performance and identify market conditions.

## 4. Key Features
- **Automated Data Pipeline:** Daily stock price fetching and stockPrice management using Spring Batch (integrating with KRX and yfinance).
- **AI-Powered Analysis:** Generation of market reports and stock analysis based on pre-calculated technical indicators (RSI, MACD, etc.).
- **Portfolio Simulation:** Robust portfolio creation and simulation tools with backtesting capabilities.

## 5. Unique Value Proposition & Strategic Pillars
- **Sophisticated Asset Allocation Simulation:**
    - Goes beyond simple return tracking to simulate how a user's target asset weight (100% basis) changes with market fluctuations.
    - Focuses on identifying optimal rebalancing timing and managing volatility risk.
    - Utilizes **Java 21 Virtual Threads** to efficiently handle complex portfolio calculations for a large user base.
- **Data-Driven Rational Decision Support:**
    - Automates the collection and pre-calculation of key technical indicators (RSI, MACD) via Spring Batch.
    - AI leverages this refined data to provide objective insights, helping users avoid emotional "impulse trading."
- **Engineering Excellence:**
    - **Hexagonal Architecture:** Isolates business logic from technical details (DB, API) to maximize maintainability.
    - **Scalability & Reliability:** Adopts an event-driven design using **Kafka** to ensure data consistency and leverages **Kubernetes (EKS)** for high availability, laying a solid foundation for future traffic spikes or MSA transition.
