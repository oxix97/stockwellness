# KRX regular session maintenance

## Session contract

The calendar source `CTCA0903R` provides whether a requested date is a business day; it does not return session hours. The application combines that date fact with the KRX regular-equity session schedule and stores the result in `trading_calendar`.

- Ordinary KRX regular session: 09:00–15:30 Asia/Seoul.
- Verified New Year opening dates: 2025-01-02 and 2026-01-02 opened at 10:00 and closed at 15:30.
- Verified CSAT sessions: 2025-11-13 and 2026-11-19 opened at 10:00 and closed at 16:30.
- The KR market-data schedule runs at 16:45 Asia/Seoul so these verified delayed-close sessions are complete before collection starts.
- Closed dates store no session hours.
- Open-date hours are enabled only for the reviewed 2025–2026 schedule. A KIS `Y` result outside that range fails closed until the corresponding annual KRX schedule and special sessions have been reviewed.

The application must keep one-off schedules explicit. Do not apply ordinary hours to an opening day without checking the annual exchange notice. KIS holiday success is evidence for the business-day flag only, not session-hour evidence.

Sources:

- [KRX KOSDAQ regular session and holidays](https://regulation.krx.co.kr/contents/RGL/03/03020401/RGL03020401.jsp)
- [KRX market hours and holidays](https://regulation.krx.co.kr/contents/RGL/03/03010100/RGL03010100T1.jsp)
- [KRX 2025 opening-day schedule notice, reproduced by Next Securities](https://www.next-securities.com/board/boardDetail.do?bbsCategory=NT01&bbsId=BBSMSTR_000000000240&content=4000383&nttId=11656)
- [KRX 2026 opening-day schedule notice, reproduced by Korea Investment & Securities](https://m.koreainvestment.com/main/customer/notice/Notice.jsp?cmd=TF04ga000002&num=45922)
- [KRX 2025 CSAT session notice, reproduced by Yuanta Securities](https://www.myasset.com/myasset/customer/notice/CU_0201000_P2.cmd?SEQ=202511100903560000000004&gubun=norNotice)
- [KRX 2026 CSAT session notice, reproduced by Yuanta Securities](https://02.myasset.com/myasset/customer/notice/CU_0201000_P2.cmd?SEQ=202611131405580000000004&gubun=norNotice)

## Update procedure

When a new annual opening schedule becomes available:

1. Review the KRX annual operating schedule and special-session notice.
2. Add an explicit date override for each delayed or early session.
3. Add a regression test for the date, timezone offset, and close time.
4. Record the source and review date here.

If an extraordinary schedule change is announced after this review, update the calendar before collecting that session.
