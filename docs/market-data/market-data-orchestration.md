# Canonical market-data orchestration

## Scheduled runs

- KR runs at 16:45 Asia/Seoul on weekdays. The extra margin covers verified 16:30 KRX closes.
- US runs at 16:15 America/New_York on weekdays and collects the regular-session close only.
- Each run prepares the relevant market calendar before source collection, then runs source jobs before derived data.
- KR runs the stock master, stock EOD, investor EOD, index/sector EOD, and derived-data jobs. US runs supported US index EOD and derived-data jobs.
- The orchestration clamps requested end dates to the latest verified completed session. Unknown session hours, unsupported US calendar years, and incomplete source results fail the run.

## Manual run endpoint

`POST /api/v1/admin/batch/market-data/sync` accepts an explicit market, ISO date range, and optional run ID:

```json
{
  "market": "KR",
  "startDate": "2026-11-02",
  "endDate": "2026-11-13",
  "runId": "kr-backfill-2026-11"
}
```

The request must include `X-Market-Data-Token`. Configure the shared secret through the runtime environment variable `STOCKWELLNESS_BATCH_MARKET_DATA_SYNC_TOKEN`, which binds to `stockwellness.batch.market-data.sync-token`.

- A missing or blank configuration rejects every request with HTTP 401.
- A missing or incorrect request header also returns HTTP 401 and does not start a job.
- The token is compared without logging it and must not be placed in a URL, request body, source control, or deployment file. Supply it through the runtime secret manager.
- Changing the token requires restarting the application. No production secret or infrastructure setting is included in this repository change.
- The manual operation uses the same market calendar and source-before-derived ordering as the scheduler; it cannot request future or unverified sessions.

## Recovery

Inspect each returned stage status and the Spring Batch execution record before retrying. Re-running the same market, date range, and run ID is idempotent at the job-instance level. A failed or incomplete source stage must be investigated before derived jobs are re-run.
