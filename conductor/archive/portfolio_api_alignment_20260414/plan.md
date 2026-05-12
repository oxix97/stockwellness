# Implementation Plan: Portfolio API Alignment

## Phase 1: Setup & Investigation
- [x] Task: Create feature branch for the task
    - [x] Create branch `chore/#<issue_number>-portfolio-api-alignment` (Done)
- [x] Task: Investigate current backend response formats
    - [x] Run local API server and inspect the responses for the 7 target endpoints. (Verified via DTO/Controller inspection)
    - [x] Verify that legacy frontend fields (`lastUpdated`, `category`, `ratio`) are not present. (Confirmed)
    - [x] Inspect specific field structures (e.g., `assetRatios`, `rebalancing`, `inception/chart`). (Confirmed)

## Phase 2: REST Docs Test Verification & Fixes
- [x] Task: Audit and update `PortfolioControllerTest` and `PortfolioAnalysisControllerTest`
    - [x] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/analysis/summary`
        - [x] Ensure `assetRatios`, `sectorRatios`, `countryRatios` use `name`/`value` format in docs.
        - [x] Ensure `rebalancing` has no `lastUpdated` field in docs.
    - [x] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}`
    - [x] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/health`
    - [x] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/advice/latest`
    - [x] Check REST Docs test for `POST /api/v1/portfolios/{portfolioId}/advice`
    - [x] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/analysis/performance/inception/chart`
        - [x] Verify comparison indicator data is documented with `ticker` as key.
    - [x] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/analysis/correlation`
    - [x] Run `./gradlew :stockwellness-api:test` to ensure tests pass and generate snippets.
    - [x] Run `./gradlew updateOpenApiSpec` (or `:stockwellness-api:openapi3`) to regenerate OpenAPI spec.

## Phase 3: Documentation Update
- [x] Task: Update Screen API Mapping documentation
    - [x] Completely overwrite `docs/specs/screen-api-mapping/portfolio.md` based on actual backend DTOs and OpenAPI specs.
    - [x] Explicitly note that the main screen reuses the `summary` structure.
    - [x] Document that `analysis/diversification` and `analysis/rebalancing` endpoints exist but are not directly called by the main flow.

## Phase 4: Final Review & Commit
- [x] Task: Perform self-review
    - [x] Ensure no legacy fields were accidentally added back.
- [x] Task: Commit changes
    - [x] Propose Korean commit messages and ask for user approval.
    - [x] Commit changes.