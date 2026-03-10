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
- **Automated Data Pipeline:** Daily stock price fetching and management using Spring Batch (integrating with KIS). Orchestrated event-driven coordination via **Kafka** ensures API caches and AI insights are automatically invalidated and refreshed upon successful data sync.
- **High-Performance Price Engine:** Optimized EOD price retrieval and return rate calculation using hierarchical Redis caching and in-memory aggregation.
- **AI-Powered Analysis:** Generation of market reports and stock analysis based on pre-calculated technical indicators (RSI, MACD, etc.).
- **Robust Deployment Architecture:** n8n을 활용한 배포 오케스트레이션과 Slack 상세 진단 알림을 통해 배포 안정성(Stability Check) 및 운영 가시성(Diagnostic Monitoring)을 확보합니다.
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
    - **Multi-Module Hexagonal Architecture:** Enforces strict boundaries between core domain logic, web adapters (API), and batch processors via a multi-module Gradle structure to maximize maintainability and failure isolation.
    - **Scalability & Reliability:** Adopts an event-driven design using **Kafka** to ensure data consistency and leverages **Kubernetes (EKS)** for high availability, laying a solid foundation for future traffic spikes or MSA transition.
