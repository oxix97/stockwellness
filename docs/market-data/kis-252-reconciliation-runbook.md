# KIS 252거래일 수집·대사 및 배포 경로 재검증

작성일: 2026-11-15
Issue: [#328 KIS 신규 DB 통합 인수 및 전환·롤백 준비](https://github.com/oxix97/stockwellness/issues/328)
선행 계약 이슈: #320

## 현재 판정

- `POST /api/v1/admin/batch/market-data/sync`에 선택적 KRX 종목 코드 `ticker`를 추가했다. 지정 종목 실행은 전체 종목 마스터와 시장 파생 배치를 생략하고, 해당 종목의 EOD·당일 투자자 수급 및 국내 지수/업종 수집을 실행한다. 미지정 실행의 기존 전체 수집 동작은 유지한다.
- PostgreSQL 15 통합 테스트가 252개의 생성 세션을 배치 서비스에 흘려보내 저장·재실행 멱등성을 확인한다. 그중 하루는 비밀을 제거한 실제 KIS 응답의 숫자를 사용해 DB 값을 필드별로 비교한다. 나머지 251일은 테스트 생성값이며, 이 결과는 252일 실 KIS 수집 증거가 아니다.
- `FHPTJ04160001`의 투자자 금액 단위(백만원)와 수량 단위(주)는 KIS 전체 문서 내보내기 사본으로 확인했다. 다른 가격·거래대금·지수·업종 필드의 단위/정밀도와 공급자 FINAL 규칙은 미확정이다. 상세 근거는 [계약 근거 재확인](2026-11-15-kis-contract-evidence-recheck.md)에 기록했다.
- 이 작업 공간에 KIS 인증정보가 없고, 사용 가능한 운영 인프라 저장소/실행 환경도 없다. KIS 실호출, 격리된 사용자 지정 PostgreSQL에서의 252일 적재, 배포 후 실제 요청 검증은 수행하지 않았다. Issue #328 인수 완료로 표시하면 안 된다.

## 브랜치와 커밋 흐름

- 기준: GitHub Issue `#328`에 대응하는 `issue/#328-kis-data-verification` 통합 브랜치를 최신 `origin/develop` `77f491d7`에서 만들고, `feature/328-kis-data-verification` 아래 `task/328-kis-data-verification`에서 작업한다. 현재 로컬에는 세 브랜치가 있고 구현은 task 브랜치에 있다.
- 완료 후 `task → feature → issue` 순서로 `--no-ff` 병합한다. PR 대상은 저장소 규칙에 따라 `develop`이다. 이 작업에서는 push, PR, develop 병합, Issue 상태 변경, 배포를 수행하지 않는다.
- 기능 단위 커밋 메시지 예시:
  - `feat(batch): #328 KRX 수동 수집에 종목 범위 지정 추가`
  - `test(kis-ingestion): #328 PostgreSQL 252세션 적재와 캡처 행 대사 추가`
  - `docs(kis-contract): #320 투자자 단위 근거와 FINAL 미확정 기록`
  - `docs(kis-runbook): #328 252일 대사·배포 재검증 절차 정리`
- 실제 커밋 전에는 정확한 메시지와 포함 파일 목록을 따로 제시해 승인을 받는다.

## 격리 PostgreSQL에서의 실 KIS 실행 절차

1. **대상 확정:** KRX 대표 종목부터 한 종목씩 실행한다. 초기 후보는 `005930`, `000660`, `035720`, `000325`, `487950`, `069500`, 대표 KOSDAQ 종목, 상장 후 252 세션 미만인 신규 종목이다. 신규 종목은 실제 상장일 이후의 세션만 범위에 넣는다.
2. **세션 확인:** 대상 ticker가 활성 종목 마스터에 있고 비상장폐지 상태가 아닌지 확인한다. 격리 DB의 `trading_calendar`에서 선택 기간에 열린 KOSPI/KOSDAQ 세션이 정확히 252개인지 확인한다. 평일 계산값만으로 휴장일을 대체하지 않는다.
3. **호출량 사전 확인:** KIS 계정의 현재 호출 한도와 안전한 실행 속도를 확인한다. 달력이 비어 있으면 오케스트레이터가 빠진 영업일의 KIS 휴장일 조회를 추가로 실행한다. 한도와 필요한 달력이 확인되기 전에는 범위 수집을 실행하지 않는다.
4. **수집:** 인증 토큰은 비밀 관리 도구에서 주입하고 요청/로그/증거 파일에 남기지 않는다. 아래 body로 관리 배치 경로를 호출한다. `startDate`와 `endDate`는 검증한 세션 범위의 양 끝이며, `runId`는 실행마다 고유하게 지정한다.

   ```json
   {
     "market": "KR",
     "startDate": "<252번째 과거 세션>",
     "endDate": "<마지막 완료 세션>",
     "runId": "kis-252-<고유 실행 식별자>",
     "ticker": "005930"
   }
   ```

   요청은 `POST /api/v1/admin/batch/market-data/sync`와 `X-Market-Data-Token` 헤더를 사용한다. ticker 지정 실행은 종목 수집 범위를 제한하지만, 국내 지수/업종 경로는 활성 canonical 시리즈 전체를 수집한다. 대표 종목 하나로 먼저 실행하고 한도·실행시간을 확인한 다음 표본을 늘린다.

5. **배치 상태:** 응답의 `stages`에서 `kisEodStockPriceSyncJob`, `kisEodInvestorTradeSyncJob`, `kisEodIndexPriceSyncJob` 실행 결과를 저장한다. 세션 누락 또는 KIS 필드 누락이 있으면 해당 잡은 실패해야 하며, 실패 행을 완전한 값으로 간주하지 않는다.
6. **DB 대사:** 실행 후 격리 DB에서 아래 쿼리로 대상 일봉을 추출한다. 실제 KIS `output2` 원본과 `trade_date`, OHLC, `acml_vol`, `acml_tr_pbmn`을 날짜별로 비교한다. 거래량과 거래대금은 단위 계약이 확정되기 전까지 원본 숫자 문자열과 저장 숫자를 비교하고, 단위를 추정해 환산하지 않는다.

   ```sql
   SELECT p.trade_date, p.open_price, p.high_price, p.low_price, p.close_price,
          p.volume, p.trading_amount, p.data_status, p.source, p.revision
   FROM stock_price_daily p
   JOIN stock s ON s.id = p.stock_id
   WHERE s.ticker = :ticker
     AND p.quote_scope = 'KRX'
     AND p.trade_date BETWEEN :start_date AND :end_date
   ORDER BY p.trade_date;
   ```

   인수 증거에는 달력상 열린 날짜 수, DB 행 수, 누락 날짜, 중복/불완전 상태 수, 필드별 차이, 실행 ID, 이미지 revision/digest, 설정 checksum, 격리 DB 식별자를 기록한다. 모든 행은 근거가 생길 때까지 `PROVISIONAL`이어야 한다.

7. **재실행·정정:** 동일 원천값으로 별도 `runId`를 사용해 재실행했을 때 행 수와 revision이 불변인지 확인한다. KIS가 실제로 정정한 값만 준비된 격리 테스트에서 재생해 해당 키의 revision/stale 전파만 바뀌는지 확인한다. 실제 provider 정정을 임의로 만들어 실 KIS 값처럼 기록하지 않는다.

## 원본 캡처 한계

현재 일반 배치 경로는 KIS 응답을 DTO로 파싱해 저장하지만 원시 HTTP 응답 본문을 보존하지 않는다. 저장 후 별도 KIS 재조회는 같은 시점의 원본이 아니며, 기존 관측에서 성공 응답과 `mod_yn=N` 상태에서도 종가가 달라졌다. 따라서 252일 필드별 대사를 완료하려면 **수집 시점에 받은 응답을 인증정보 없이 보존하는 승인된 캡처 경로**가 필요하다. 이를 마련하지 않은 재조회값만으로 원본 대사를 통과 처리하지 않는다.

## 배포 후 실제 요청 경로

- 배포가 승인된 뒤 API와 Batch 이미지 digest, 구성 checksum, 스키마 버전을 기록하고 동일 버전이 구동 중임을 먼저 확인한다. 이 단계의 `POST /market-data/sync`는 DB 쓰기를 발생시키므로 배포 대상이 격리된 검증 DB일 때만 사용한다.
- 한 표본 ticker/짧은 완료 범위로 관리 배치 요청을 실행해 API → Batch → KIS → PostgreSQL 경로와 `stages` 결과를 확인한다. 그 뒤 읽기 전용으로 `GET /api/v1/stocks/{ticker}/prices/history?period=1Y&frequency=DAILY`를 확인한다.
- 현재 `StockPriceReadAdapter`는 `data_status = 'FINAL'`만 가격 이력으로 읽는다. KIS FINAL 근거가 없는 현재 계약에서는 저장 데이터가 PROVISIONAL인 것이 정상이며, 이 GET 요청이 데이터를 실시간/확정 가격처럼 보여주지 않는지 확인해야 한다. 최종 데이터가 없어 가격 이력이 제공되지 않는 것은 현재 안전 정책상 예상 상태다.
- 화면 확인은 해당 FE `StockDetail`의 로딩·빈 상태·오류·모바일 폭에서 수행하고, API 응답 날짜·값·EOD 표시가 같은지 기록한다. 이번 백엔드 통합 테스트는 배포, 실제 KIS 네트워크, 운영 UI를 검증하지 않는다.

## 테스트 증거

- `NewMarketDataBatchConfigIntegrationTest`는 PostgreSQL 15 Testcontainers에서 252 세션 저장, ticker 범위, 캡처 숫자 대사, 재실행 revision 불변을 확인한다.
- `MarketDataBatchOrchestratorTest`는 ticker 실행이 종목 마스터/전체 파생 잡을 건너뛰고 지정 종목을 가격·투자자 잡에 전달하는지 확인한다.
- `BatchAdminControllerTest`는 관리 요청의 ticker 전달과 비KR ticker 거부를 확인한다.
- `NewMarketDataSyncServiceTest.investor_flows_are_saved_per_type_with_signed_values_and_missing_values_left_null`는 투자자 금액의 백만원→원 환산과 누락값/PROVISIONAL 처리를 확인한다.
