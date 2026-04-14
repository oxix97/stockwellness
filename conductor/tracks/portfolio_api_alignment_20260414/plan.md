# Implementation Plan: Portfolio API Alignment

## Phase 1: Setup & Investigation
- [ ] Task: Create feature branch for the task
    - [ ] Create branch `chore/#<issue_number>-portfolio-api-alignment`
- [ ] Task: Investigate current backend response formats
    - [ ] Run local API server and inspect the responses for the 7 target endpoints.
    - [ ] Verify that legacy frontend fields (`lastUpdated`, `category`, `ratio`) are not present.
    - [ ] Inspect specific field structures (e.g., `assetRatios`, `rebalancing`, `inception/chart`).

## Phase 2: REST Docs Test Verification & Fixes
- [ ] Task: Audit and update `PortfolioControllerTest` and `PortfolioAnalysisControllerTest`
    - [ ] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/analysis/summary`
        - [ ] Ensure `assetRatios`, `sectorRatios`, `countryRatios` use `name`/`value` format in docs.
        - [ ] Ensure `rebalancing` has no `lastUpdated` field in docs.
    - [ ] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}`
    - [ ] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/health`
    - [ ] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/advice/latest`
    - [ ] Check REST Docs test for `POST /api/v1/portfolios/{portfolioId}/advice`
    - [ ] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/analysis/performance/inception/chart`
        - [ ] Verify comparison indicator data is documented with `ticker` as key.
    - [ ] Check REST Docs test for `GET /api/v1/portfolios/{portfolioId}/analysis/correlation`
    - [ ] Run `./gradlew :stockwellness-api:test` to ensure tests pass and generate snippets.
    - [ ] Run `./gradlew updateOpenApiSpec` (or `:stockwellness-api:openapi3`) to regenerate OpenAPI spec.

## Phase 3: Documentation Update
- [ ] Task: Update Screen API Mapping documentation
    - [ ] Completely overwrite `docs/specs/screen-api-mapping/portfolio.md` based on actual backend DTOs and OpenAPI specs.
    - [ ] Explicitly note that the main screen reuses the `summary` structure.
    - [ ] Document that `analysis/diversification` and `analysis/rebalancing` endpoints exist but are not directly called by the main flow.

## Phase 4: Final Review & Commit
- [ ] Task: Perform self-review
    - [ ] Ensure no legacy fields were accidentally added back.
- [ ] Task: Commit changes
    - [ ] Propose Korean commit messages and ask for user approval.
    - [ ] Commit changes.