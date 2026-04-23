#!/usr/bin/env bash

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.k6.yaml"
RESULTS_DIR="$SCRIPT_DIR/results"
MODE="${1:-standard}"
K6_MODE="standard"
if [[ "$MODE" == "quick" ]]; then
  K6_MODE="quick"
fi
RUN_TS="$(date '+%Y%m%d-%H%M%S')"
SUMMARY_FILE="$RESULTS_DIR/run-all-summary-${RUN_TS}.md"
LOG_DIR="$RESULTS_DIR/logs/$RUN_TS"
OVERALL_EXIT_CODE=0
declare -a SUMMARY_LINES=()

SMOKE_SCENARIOS=(
  "smoke:smoke-live"
)

PUBLIC_READ_SCENARIOS=(
  "market-indexes:market-indexes-standard"
  "stock-search:stock-search-standard"
  "stock-popular-search:stock-popular-search-standard"
  "stock-new-listings:stock-new-listings-standard"
  "stock-detail:stock-detail-standard"
  "stock-supply-ranking:stock-supply-ranking-standard"
  "stock-price-history:stock-price-history-standard"
  "stock-returns:stock-returns-standard"
  "sector-fluctuation-ranking:sector-fluctuation-ranking-standard"
  "sector-comparison:sector-comparison-standard"
  "sector-detail:sector-detail-standard"
)

AUTH_READ_SCENARIOS=(
  "member-me:member-me-standard"
  "member-notifications:member-notifications-standard"
  "portfolio-list:portfolio-list-standard"
  "portfolio-detail:portfolio-detail-standard"
  "portfolio-health:portfolio-health-standard"
  "portfolio-advice-latest:portfolio-advice-latest-standard"
  "analysis-valuation:analysis-valuation-standard"
  "analysis-diversification:analysis-diversification-standard"
  "analysis-rebalancing:analysis-rebalancing-standard"
  "analysis-summary:analysis-summary-standard"
  "analysis-correlation:analysis-correlation-standard"
  "analysis-inception-performance:analysis-inception-performance-standard"
  "analysis-inception-chart:analysis-inception-chart-standard"
  "watchlist-groups:watchlist-groups-standard"
  "watchlist-items:watchlist-items-standard"
  "stock-search-history:stock-search-history-standard"
)

WRITE_SCENARIOS=(
  "auth-login:auth-login-once"
  "portfolio-create:portfolio-create-once"
  "watchlist-group-create:watchlist-group-create-once"
)

usage() {
  cat <<'EOH'
Usage:
  ./run-all.sh [standard|quick|write|all]

Modes:
  standard  Full load test (smoke + public read + auth read)
  quick     Short smoke test for verification (smoke + public read + auth read)
  write     One-shot write helper scenarios only
  all       standard + write
EOH
}

append_summary() {
  local line="$1"
  SUMMARY_LINES+=("$line")
}

extract_metric() {
  local result_file="$1"
  local jq_expr="$2"
  if [[ ! -f "$result_file" ]] || ! command -v jq >/dev/null 2>&1; then
    echo "N/A"
    return
  fi
  jq -r "$jq_expr // \"N/A\"" "$result_file" 2>/dev/null || echo "N/A"
}

write_summary_file() {
  {
    echo "# k6 run-all summary"
    echo ""
    echo "- run_at: $RUN_TS"
    echo "- mode: $MODE"
    echo "- k6_mode: $K6_MODE"
    echo "- log_dir: $LOG_DIR"
    echo ""
    echo "| scenario | status | exit_code | p95(ms) | p99(ms) | failed_rate | request_count | checks |"
    echo "|---|---|---:|---:|---:|---:|---:|---:|"
    for line in "${SUMMARY_LINES[@]-}"; do
      echo "$line"
    done
  } > "$SUMMARY_FILE"
}

print_summary() {
  echo ""
  echo "### Summary"
  echo "summary file: $SUMMARY_FILE"
  echo ""
  printf '%-32s %-18s %-9s %-12s %-12s %-12s %-14s %-8s\n' \
    "scenario" "status" "exit_code" "p95(ms)" "p99(ms)" "failed_rate" "request_count" "checks"
  for line in "${SUMMARY_LINES[@]-}"; do
    IFS='|' read -r _ scenario status exit_code p95 p99 failed_rate request_count checks _ <<< "$line"
    printf '%-32s %-18s %-9s %-12s %-12s %-12s %-14s %-8s\n' \
      "$scenario" "$status" "$exit_code" "$p95" "$p99" "$failed_rate" "$request_count" "$checks"
  done
}

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Missing file: $path" >&2
    exit 1
  fi
}

run_scenario() {
  local scenario_name="$1"
  local result_name="$2"
  local result_file="$RESULTS_DIR/${result_name}.json"
  local log_file="$LOG_DIR/${result_name}.log"
  local exit_code=0
  local status="passed"

  echo ""
  echo "==> Running ${scenario_name}.js (mode: $K6_MODE)"
  rm -f "$result_file" "$log_file"
  docker compose -f "$COMPOSE_FILE" run --rm \
    k6 run \
    -e K6_MODE="$K6_MODE" \
    --summary-export "/scripts/results/${result_name}.json" \
    "/scripts/scenarios/${scenario_name}.js" \
    2>&1 | tee "$log_file"
  exit_code=${PIPESTATUS[0]}

  if [[ $exit_code -ne 0 ]]; then
    OVERALL_EXIT_CODE=1
    status="failed"
  fi

  local p95="$(extract_metric "$result_file" '.metrics.http_req_duration["p(95)"]')"
  local p99="$(extract_metric "$result_file" '.metrics.http_req_duration["p(99)"]')"
  local failed_rate="$(extract_metric "$result_file" '.metrics.http_req_failed.value')"
  local request_count="$(extract_metric "$result_file" '.metrics.http_reqs.count')"
  local checks="$(extract_metric "$result_file" '.metrics.checks.value')"

  append_summary "| ${scenario_name} | ${status} | ${exit_code} | ${p95} | ${p99} | ${failed_rate} | ${request_count} | ${checks} |"
}

run_group() {
  local group_name="$1"
  shift
  local scenarios=("$@")
  echo ""
  echo "### ${group_name}"
  for item in "${scenarios[@]}"; do
    local scenario_name="${item%%:*}"
    local result_name="${item##*:}"
    run_scenario "$scenario_name" "$result_name"
  done
}

main() {
  if [[ "$MODE" == "standard" || "$MODE" == "quick" || "$MODE" == "all" ]]; then
    "$SCRIPT_DIR/refresh-token.sh"
  fi
  require_file "$COMPOSE_FILE"
  require_file "$SCRIPT_DIR/.env"
  mkdir -p "$RESULTS_DIR"
  mkdir -p "$LOG_DIR"

  case "$MODE" in
    standard|quick)
      run_group "Smoke" "${SMOKE_SCENARIOS[@]}"
      run_group "Public Read APIs" "${PUBLIC_READ_SCENARIOS[@]}"
      run_group "Authenticated Read APIs" "${AUTH_READ_SCENARIOS[@]}"
      ;;
    write)
      run_group "Write Helper Scenarios" "${WRITE_SCENARIOS[@]}"
      ;;
    all)
      run_group "Smoke" "${SMOKE_SCENARIOS[@]}"
      run_group "Public Read APIs" "${PUBLIC_READ_SCENARIOS[@]}"
      run_group "Authenticated Read APIs" "${AUTH_READ_SCENARIOS[@]}"
      run_group "Write Helper Scenarios" "${WRITE_SCENARIOS[@]}"
      ;;
    *)
      usage
      exit 1
      ;;
  esac

  write_summary_file
  print_summary
  exit "$OVERALL_EXIT_CODE"
}

main "$@"
