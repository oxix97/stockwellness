# US index session calendar maintenance

## Covered data

KIS-backed US index collection is limited to S&P 500 (`NYSE`), Nasdaq 100 (`NASDAQ`), and Dow Jones Industrial Average (`NYSE`). `COMP` and `SOX` do not have a KIS source in the verified contract and must not be scheduled.

The application currently contains the published 2025–2027 cash-equity schedules for NYSE and Nasdaq. It records weekdays, exchange holidays, regular sessions (09:30–16:00 America/New_York), 2025 early closes on July 3, November 28, and December 24, 2026 early closes on November 27 and December 24, and the 2027 early close on November 26. July 2, 2026 is a regular session; Independence Day is observed on Friday, July 3. In 2027, Juneteenth (June 18), Independence Day (July 5), and Christmas Day (December 24) are observed exchange holidays, and November 26 is the sole 13:00 ET early close. The 2025 schedule also records the January 9 National Day of Mourning closure announced by Nasdaq. Session instants are derived from the IANA `America/New_York` zone so daylight-saving offsets are not hard-coded.

Nasdaq currently expects to add an overnight session beginning Sunday, December 6, 2026, subject to SIP readiness and applicable SEC rule changes. The canonical KIS EOD collection continues to use the regular-session close at 16:00 ET. Overnight or extended-session observations must not be relabeled as regular-session EOD; revisit the KIS source contract before changing this basis.

Nasdaq's official 2025, 2026, and 2027 schedules are the source for Nasdaq sessions. The NYSE Group's 2025–2027 announcement and NYSE Hours & Calendars are the source for NYSE holiday and early-close dates. Nasdaq's January 9, 2025 closure notice is included as an ad hoc event. The 2027 schedule was verified and added on 2026-11-26. Same-day emergency closures are not predictable from the annual schedule and require an explicit reviewed calendar update before collection resumes.

Sources:

- [Nasdaq 2025 U.S. Holiday & Trading Hours PDF](https://www.nasdaq.com/docs/2025/01/06/2025holidayandtradinghours.pdf)
- [Nasdaq 2026 U.S. Holiday & Trading Hours](https://www.nasdaq.com/holiday-trading-hours)
- [Nasdaq 2027 Trading Calendar PDF](https://www.nasdaq.com/docs/2027-trading-calendar) (accessed 2026-11-26)
- [Nasdaq 23/5 Trading Information Hub](https://www.nasdaq.com/23-5-trading)
- [Nasdaq January 9, 2025 National Day of Mourning closure](https://ir.nasdaq.com/news-releases/news-release-details/nasdaq-announces-closure-its-us-markets-honor-national-day-0)
- [NYSE Group 2025–2027 holiday and early-close announcement](https://www.nasdaq.com/press-release/nyse-group-announces-2025-2026-and-2027-holiday-and-early-closings-calendar-2024-11)
- [NYSE Hours & Calendars](https://www.nyse.com/trade/hours-calendars) (accessed 2026-11-26)

## Fail-closed behavior

The calendar refuses unsupported markets and years. Its verified range currently covers 2025 through 2027 (2028 and later fail closed). Do not replace this with a weekday-only guess: an unknown holiday or early close could cause an incomplete EOD observation to be requested and stored. The batch must stop before KIS access when the requested session is outside the verified schedule.

## Annual update procedure

Before enabling a new calendar year:

1. Open Nasdaq's published U.S. holiday schedule and NYSE's hours calendar. Record the access date and the source revision in the calendar implementation and this document.
2. Verify closures and early closes independently for each configured index's exchange (`NASDAQ` for NDX; `NYSE` for SPX and `.DJI`). Include any announced one-off closure or special session.
3. Add the new year's dated schedule and keep it bounded by an explicit supported-year check.
4. Add tests for all closed and early-close dates, the final normal session before and after daylight-saving transitions, and the first/last supported date.
5. Run the batch calendar tests and inspect the generated schedule against the official exchange source before deploying a release that crosses the year boundary.

If an official next-year schedule has not been reviewed, collection for that year remains disabled and the batch reports the unsupported year. The 2025–2027 schedule is intentionally bounded; it is not presented as a live exchange feed.
