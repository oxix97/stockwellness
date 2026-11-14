# KIS source and session contract — 2026-11-13

## Observed price response

The captured fixture `stockwellness-core/src/test/resources/kis/source-contract/domestic-daily-success.json` is a sanitized row from the read-only production audit. Its source capture SHA-256 is recorded in the fixture. It contains no access token, app key, account, or member data.

- Endpoint: domestic daily item chart price, `FHKST03010100`.
- Request market code `J`, date range `20261112`–`20261112`, period `D`, and `FID_ORG_ADJ_PRC=1`.
- The official KIS sample documents `FID_ORG_ADJ_PRC=0` as adjusted price and `1` as raw price. It also states that one request returns up to 100 observations. Query windows therefore must keep each requested range below that cap; the current service bounds windows by 100 calendar days.
- The response fields used by the current adapter are `stck_bsop_date`, `stck_oprc`, `stck_hgpr`, `stck_lwpr`, `stck_clpr`, `acml_vol`, and `acml_tr_pbmn`. Preserve the provider's reported values and the date in the response. The captured sample alone does not establish every field's unit or rounding rules; unit confirmation remains open.
- KIS `rt_cd=0` means the request was processed successfully. The reviewed sample labels `mod_yn` as a change flag, but neither that flag nor the response includes a final/settled status field.
- The captured response is dated 2026-11-12 and was collected at 2026-11-12 16:42 UTC, after the KRX regular close. It has `mod_yn=N`. The service stores this complete row as `PROVISIONAL`; a post-close successful response is not treated as evidence of finality.

Sources: [KIS domestic daily item chart sample](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/inquire_daily_itemchartprice.py), [KIS API list](https://apiportal.koreainvestment.com/apiservice-summary).

### Same-query re-read observation

A read-only KIS request on 2026-11-13 repeated the captured 2026-11-12 request with the same ticker, date range, daily period, and raw-price flag. The saved 2026-11-12 16:42:47 UTC response had close `206500`; the re-read returned close `208000`. Open, high, low, volume, and trading amount were unchanged, and both responses had `rt_cd=0` and `mod_yn=N`. The re-read response still had no provider finality field. This proves the successful after-close response and `mod_yn=N` do not establish that the row is final. It does not establish when KIS considers the value settled, so both observations remain PROVISIONAL. Only selected market fields were retained; no credential, token, or database write was used.

### Overseas index response observation

The sanitized fixture `stockwellness-core/src/test/resources/kis/source-contract/overseas-index-daily-2026-11-12.json` records selected fields from read-only live responses queried on 2026-11-13 for 2026-11-12. SPX, NDX, and `.DJI` each returned HTTP/API success (`rt_cd=0`), one dated row, and `mod_yn=N`. SPX and NDX reported `acml_vol=0`; `.DJI` reported `1015162860`. The fixture preserves those values without interpreting the zero volumes as missing or as proof of no trading. The sample did not provide a finality field, so these observations remain PROVISIONAL.

## Other API surfaces and supported series

- Investor trades use `inquire-investor` (`FHKST01010900`). The KIS example describes person/foreign/institution data and says same-day values are provided after market close. This timing statement is not a finality marker. The source sample does not specify all stored unit/precision rules for the planned investor categories.
- Domestic index daily observations use `inquire-daily-indexchartprice` (`FHKUP03500100`). Current code also uses this index series endpoint for the new domestic index path.
- The official KIS overseas index chart sample says its US index path supports only Dow 30, Nasdaq 100, and S&P 500. The current backend `BenchmarkType` lists five overseas series: SPX, COMP, NDX, SOX, and .DJI. The product decision is to collect only the three KIS-supported series and record COMP/SOX as unsupported. Do not synthesize values for unsupported series or mark them FINAL.
- Nasdaq has announced 23/5 trading from 2026-12-06. The product decision is to keep EOD on the regular 09:30–16:00 ET core-session basis. This is a collection policy; KIS has not documented the daily series' settlement/finality semantics, so it does not provide evidence to promote rows to FINAL.

Sources: [KIS investor API sample](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_investor/inquire_investor.py), [KIS overseas index sample and limits](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/overseas_stock/inquire_daily_chartprice/chk_inquire_daily_chartprice.py), [KIS domestic index endpoint reference](https://github.com/koreainvestment/open-trading-api/blob/main/backtester/kis_backtest/providers/kis/constants.py), [Nasdaq 2026 holiday schedule](https://www.nasdaq.com/market-activity/stock-market-holiday-schedule), [Nasdaq 23/5 announcement](https://www.nasdaq.com/23-5-trading).

## Session evidence

`stockwellness-core/src/test/resources/kis/source-contract/market-sessions-2026.json` captures selected official session examples and their UTC conversions:

- KRX regular session is 09:00–15:30 Asia/Seoul; May 1 is Labor Day and closed. The sample does not represent NXT or after-market sessions.
- US equity core session is 09:30–16:00 America/New_York. The samples cover the 2026 DST boundary, Good Friday closure, and the 2026-11-27 13:00 early close.
- The application must convert each verified local session through the IANA zone and persist only source-backed times. These sample rows are regression references, not a full future holiday calendar.

Sources: [KRX trading hours and holidays](https://regulation.krx.co.kr/contents/RGL/03/03010100/RGL03010100T1.jsp), [Nasdaq holidays, hours, and early closes](https://www.nasdaq.com/market-activity/stock-market-holiday-schedule), [NYSE holidays and hours](https://www.nyse.com/trade/hours-calendars).

## Finality decision

No reviewed KIS API sample or captured response exposes a provider-issued finality marker. Do not promote a row because the HTTP/API call succeeded, a session is closed, it was fetched again, or `mod_yn` is `N`. Keep such data `PROVISIONAL` and exclude it from user-visible and derived reads until the provider supplies a documented finality signal or authoritative rule that the implementation can test.

## Still unresolved — blocks Task #320 and final cutover PR

1. KIS documentation or an authoritative response field that establishes when an EOD row is FINAL.
2. Provider-defined units, precision, and adjustment semantics for all persisted price, trading amount, investor flow, index, and sector fields.
3. Full source/API contracts for investor, index, and sector collection, including whether status/revision fields can be supported by KIS evidence.
4. A maintained holiday/early-close source and update mechanism for every configured US index calendar. The core-session policy is decided; this sample does not supply a maintained full calendar.

## Product decisions recorded 2026-11-13

- After 2026-12-06, use the regular 09:30–16:00 ET session for US index EOD.
- Collect KIS-supported SPX, NDX, and .DJI. Record COMP and SOX as unsupported by the reviewed KIS source; do not synthesize or silently substitute another provider.

The finality gap is intentional and visible: no FINAL evidence has been inferred. Task #320 remains open; any data without provider proof remains PROVISIONAL.

## KIS-owned source recheck (2026-11-15)

Reviewed the public KIS Developers API index and KIS-owned `open-trading-api` examples/configuration on 2026-11-15 (Asia/Seoul). No authenticated API call was made. The repository examples demonstrate request construction and field labels; they are not a substitute for a complete endpoint response schema.

### Documented facts

- For `inquire-daily-itemchartprice` (`FHKST03010100`), KIS's sample documents a maximum of 100 observations per request and defines `FID_ORG_ADJ_PRC=0` as adjusted price and `1` as raw price. The separate `inquire-daily-price` sample defines the same-named flag in the opposite wording (`0` unadjusted, `1` adjusted) and limits that endpoint to recent periods. Keep the mapping scoped to the exact endpoint used; the samples do not define the corporate-action adjustment methodology. Sources: [daily item chart request sample](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/inquire_daily_itemchartprice.py), [daily item chart field mapping](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/chk_inquire_daily_itemchartprice.py), [separate daily-price request sample](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/domestic_stock/inquire_daily_price/inquire_daily_price.py).
- The KIS domestic daily and index check samples label OHLC/date fields, `acml_vol` as accumulated volume, `acml_tr_pbmn` as accumulated trading amount, and `mod_yn` as “변경 여부” (change indicator). The overseas index check sample also labels `mod_yn` as a change indicator. The investor example says same-day data is available after market close, defines “foreign” as registered foreign investors plus other foreign investors, and labels person/foreign/institution net quantity and trading-amount fields. Its check sample leaves `NUMERIC_COLUMNS` empty because the sample says data types are not specified in its metadata. Sources: [domestic daily field mapping](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/chk_inquire_daily_itemchartprice.py), [domestic index field mapping](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/domestic_stock/inquire_daily_indexchartprice/chk_inquire_daily_indexchartprice.py), [investor request and timing notes](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/domestic_stock/inquire_investor/inquire_investor.py), [investor field mapping](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/domestic_stock/inquire_investor/chk_inquire_investor.py), [overseas index field mapping and supported US indices](https://raw.githubusercontent.com/koreainvestment/open-trading-api/main/examples_llm/overseas_stock/inquire_daily_chartprice/chk_inquire_daily_chartprice.py).
- The KIS public API index lists these endpoint categories and names. The reviewed public page and repository examples do not state a FINAL status field, a settlement/finality time, a `mod_yn` value-to-revision rule, or a correction retention window. Source: [KIS Developers API index](https://apiportal.koreainvestment.com/apiservice-summary).

### Interpretation and remaining evidence gap

“Available after market close” is a publication-time note, not a provider statement that the value is settled. Likewise, `mod_yn` being labelled as a change indicator does not define FINAL or establish that `N` means immutable. The previously recorded same-query reread, where the successful post-close close changed while `mod_yn=N`, is consistent with keeping both observations PROVISIONAL; this is an implementation inference, not a KIS finality rule.

The reviewed examples translate field names but do not provide authoritative units, numeric precision, rounding rules, or a full price-adjustment methodology for the persisted price, index, volume, trading-amount, and investor fields. This bounded review of public portal material and KIS-owned examples does not prove that no additional specification exists in the portal's downloadable endpoint guides or authenticated materials. FINAL promotion and unit normalization therefore remain blocked on an authoritative endpoint specification or KIS clarification.
