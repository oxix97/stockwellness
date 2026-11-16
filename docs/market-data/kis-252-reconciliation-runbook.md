# KIS 252거래일 수집·대사 및 배포 경로 재검증

작성일: 2026-11-16
Issue: [#328 KIS 신규 DB 통합 인수 및 전환·롤백 준비](https://github.com/oxix97/stockwellness/issues/328)
선행 계약 이슈: #320

## 현재 판정

- `POST /api/v1/admin/batch/market-data/sync`의 선택적 KRX 종목 코드 `ticker`를 사용해 격리 PostgreSQL 15에 실제 KIS 252거래일을 적재했다. KIS 달력 기준 범위는 `2025-11-04..2026-11-13`이며, KOSPI 열린 세션 252개를 DB 달력에서 확인했다.
- 격리 실행 `kis-verify-328-20261116-a`에서 `runId=kis-252-capture-20261116` 및 `runId=kis-252-capture-idempotent-20261116-b`로 두 번 호출했다. 가격·투자자·지수 단계는 모두 `COMPLETED`였다. 대상 KRX 가격은 252행/252일, 조정 가격은 252행/252일, 투자자 수급은 3,276행(252일×13유형)이었다. 실제 배치 HTTP 응답 본문과 PostgreSQL 저장값을 직접 대사한 결과는 각 가격 응답 숫자 1,512개, 조정 가격 1,008개, 투자자 날짜/유형별 수량·금액 3,276쌍 모두 차이 0이었다. 두 번의 실행에서 17개 요청 조합별 원본 응답 본문이 모두 동일했고, 가격·수급 행은 `PROVISIONAL`, 누락/불완전 0, revision 1이었다. 해시와 비교 범위는 [동일 응답 캡처 기록](2026-11-15-kis-contract-evidence-recheck.md)에 기록했다.
- `FHPTJ04160001`의 투자자 금액 단위(백만원)와 수량 단위(주)는 KIS 전체 문서 사본과 2026-04-10 수정 KIS Developers endpoint 문서로 확인했다. KIS 실응답은 기준일로부터 최대 30개 거래일을 반환하는 동작을 보여 서비스가 30세션 창으로 적재하도록 수정했다. 가격·거래대금·지수·업종 단위/정밀도와 공급자 FINAL 규칙은 미확정이다. 원시 가격·투자자 EOD 행은 `PROVISIONAL`이며 지수 행은 이 격리 실행에서 생성되지 않았다. 상세 근거는 [계약 근거 재확인](2026-11-15-kis-contract-evidence-recheck.md)에 기록했다.
- 새 Batch jar를 임시 격리 컨테이너에 반영하고 실제 관리 요청을 두 번 보냈다. 컨테이너 health는 `UP`, 두 응답은 HTTP 200/S000, 마지막 실행의 가격·투자자·지수 세 단계는 `COMPLETED`였다. 이는 격리된 Batch 요청 경로 검증이며 운영 배포, 운영 DB 쓰기, 운영 API·화면 확인은 아니다. KIS FINAL 근거와 재설계 스키마 호환성이 해결되지 않아 Issue #328 인수/최종 PR 및 운영 배포는 보류한다.
- 추가 격리 실행 `runId=kis-api-read-252-20261116-01`은 새 PostgreSQL 15에서 HTTP 200/S000, 26/26 `COMPLETED`로 252일을 적재했다. DB에는 열린 세션 252개, 가격 252행/252일, 조정 가격 252행, 투자자 3,276행/252일이 있었고, 가격·투자자 행은 모두 `PROVISIONAL`, `FINAL`/`INCOMPLETE` 0, 최대 revision 1이었다. 같은 DB에 API jar(`b89c04edcb3d9166de987617b6614ab36ec8f647057bd956edadfc9b1414f234`)를 연결했다. API는 `prod` 프로파일·`ddl-auto=validate`로 기동되어 health 200이었다. `GET /api/v1/stocks/005930/prices/history?period=1Y&frequency=DAILY`는 HTTP 404/S002를 반환했다. 읽기 SQL이 `FINAL`만 조회하므로 예상된 결과다. 이 검증은 격리 요청 경로이며 운영 배포 검증이 아니다.

## 스키마 통합 제한

- 현재 애플리케이션 엔티티와 수동 재설계 스키마 `stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/02-schema.sql`의 호환성은 확인되지 않았다. 해당 스키마로 만든 빈 DB에서 앱을 `ddl-auto=validate`로 시작했을 때 `advisor_reports` 부재로 검증에 실패했다. 검증을 끈 실행에서도 기존 주식 조회가 요구하는 컬럼이 없어 실패했다.
- 따라서 위 252일 요청은 테스트 fixture의 레거시 호환 스키마에 `stock_investor_trade_daily`를 추가한 임시 DB에서 확인했다. 252일 적재 결과는 수집 코드·요청 경로의 증거지만, 재설계 스키마 마이그레이션/전환의 증거가 아니다.
- 앱 엔티티와 스키마의 차이를 정리하고 비파괴 마이그레이션·롤백 및 읽기 경로를 검증하기 전까지 재설계 스키마 기반 배포와 전환은 미검증이며 진행을 막는 조건이다.

## 운영 호스트 읽기 전용 확인 — 2026-11-16

- `mini` 호스트의 API·Batch 및 PostgreSQL 컨테이너는 확인 당시 healthy였다. 실행 중인 API 이미지는 `ghcr.io/oxix97/stockwellness-api:api-v1.1.1` (`sha256:6c820d97d804b5644664d9d39e71fa836e20d511c7532029aca15acd595be5a4`), Batch 이미지는 `ghcr.io/oxix97/stockwellness-batch:1a4b0d901780dcc391f5bee1cfe456fff45e481e` (`sha256:8bd93abddfa6e87a61dfb1d5d02f777d755b17ffca332d28915df77b7c2b0ccc`)였다.
- PostgreSQL 테이블 목록에서 `stock_price_eod`, `stock_investor_trade_eod`는 확인했으나 새 적재 코드의 `stock_price_daily`, `stock_investor_trade_daily`는 확인하지 못했다. 이 확인은 메타데이터 읽기 전용이며 운영 행 데이터는 조회·수정하지 않았다.
- 현재 운영 API의 공개 경로 `GET /api/v1/stocks/005930/prices/history?period=1Y&frequency=DAILY`를 재조회해 HTTP 200, 종목 `005930`, 시세 145개를 확인했다. 이 결과는 당시 실행 중인 기존 API 이미지의 기준선이며 이번 KIS 변경의 배포 후 검증이 아니다.
- 운영 DB의 스키마 이행 전에는 새 Batch 이미지를 배포하거나 252일 적재 요청을 보내지 않는다. 위 임시 격리 실행은 운영 컨테이너·DB 경로를 검증한 것이 아니다.

## 브랜치와 커밋 흐름

- 기준: GitHub Issue `#328`에 대응하는 `issue/#328-kis-data-verification` 통합 브랜치를 최신 `origin/develop` `77f491d7`에서 만들고, `feature/328-kis-data-verification` 아래 `task/328-kis-data-verification`에서 작업한다. 현재 로컬에는 세 브랜치가 있고 구현은 task 브랜치에 있다.
- 완료 후 승인된 커밋을 `task → feature → issue` 순서로 로컬 `--ff-only` 병합한다. `develop` 병합은 하지 않는다. 현재 새 변경 커밋은 저장소 규칙에 따른 메시지·파일 승인 전이다. 공급자 FINAL 근거와 운영 스키마 호환·실제 배포 경로 검증이 해결되기 전에는 최종 PR을 만들지 않는다.
- 기능 단위 커밋 메시지 예시:
  - `feat(batch): #328 KRX 수동 수집에 종목 범위 지정 추가`
  - `test(kis-ingestion): #328 PostgreSQL 252세션 적재와 캡처 행 대사 추가`
  - `docs(kis-contract): #320 투자자 단위 근거와 FINAL 미확정 기록`
  - `docs(kis-runbook): #328 252일 대사·배포 재검증 절차 정리`
  - `fix(kis-ingestion): #328 30세션 투자자 수급 적재 및 누락 검증`
  - `feat(kis-evidence): #328 KIS 응답 본문 선택 캡처 추가`
  - `docs(kis-reconciliation): #328 KIS 252일 대사 및 API 경로 기록`
- 실제 커밋 전에는 정확한 메시지와 포함 파일 목록을 따로 제시해 승인을 받는다.

## 격리 PostgreSQL에서의 실 KIS 실행 절차

1. **대상 확정:** KRX 대표 종목부터 한 종목씩 실행한다. 초기 후보는 `005930`, `000660`, `035720`, `000325`, `487950`, `069500`, 대표 KOSDAQ 종목, 상장 후 252 세션 미만인 신규 종목이다. 신규 종목은 실제 상장일 이후의 세션만 범위에 넣는다.
2. **세션 확인:** 대상 ticker가 활성 종목 마스터에 있고 비상장폐지 상태가 아닌지 확인한다. 격리 DB의 `trading_calendar`에서 선택 기간에 열린 KOSPI/KOSDAQ 세션이 정확히 252개인지 확인한다. 평일 계산값만으로 휴장일을 대체하지 않는다.
3. **호출량 사전 확인:** KIS 클라이언트 제한값은 100ms당 1회(초당 최대 10회)다. 달력이 비어 있으면 오케스트레이터가 빠진 날짜의 KIS 휴장일 조회를 추가로 실행한다. 실제 252 세션 실행에서는 약 1년 범위의 달력 준비가 포함되므로 계정별 호출 한도와 필요한 달력 범위를 확인한 후 실행한다.
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

## 동일 HTTP 응답 캡처 및 대사

KIS 어댑터에 선택적 수집 시점 캡처를 추가했다. 격리 Batch 컨테이너에 `KIS_EVIDENCE_CAPTURE_DIRECTORY`를 설정하면 종목 일봉 가격(원시·조정) 및 투자자 일봉 API의 HTTP 응답 본문을 DTO 파싱 전에 `.body`로 보존하고, 요청 경로·쿼리·TR ID·상태·시각·본문 SHA-256을 `.json` 메타데이터에 기록한다. 캡처는 기본 비활성이고 이 두 공개 시장 데이터 경로만 허용한다. 인증 헤더, App Key/Secret은 저장하지 않는다. 캡처 디렉터리 쓰기에 실패하면 해당 KIS 요청도 실패시켜 증거 누락 상태로 적재하지 않는다.

2026-11-16 격리 실행에서 17개 요청 조합(가격 4개 범위×원시/조정 2종, 투자자 9개 기준일)을 두 번 호출해 원본 body 34개와 메타데이터를 확인했다. 메타데이터 SHA-256과 파일 SHA-256이 모두 일치했고, 17개 조합은 두 실행 간 body 해시도 동일했다. 실행 뒤 임시 캡처 파일은 비밀 및 불필요한 시장 데이터 보존을 피하기 위해 정리했다. 해시별 상세와 동일 응답 대사 결과는 계약 근거 문서에 기록했다. 과거 관측에서 성공 응답과 `mod_yn=N` 상태에서도 종가가 달라졌으므로 재조회나 해당 플래그만으로 FINAL을 판정하지 않는다.

## 배포 후 실제 요청 경로

- 격리 검증에서는 Batch jar SHA-256 `ad8d23cbde81529abd231b27803ee2a47e77c437a22acf7b1e84822cb3545db1`로 실제 KIS 적재를 수행했고, 별도 API 컨테이너에서 동일 DB를 읽었다. API jar SHA-256은 `b89c04edcb3d9166de987617b6614ab36ec8f647057bd956edadfc9b1414f234`이며 `prod` 프로파일에서 `ddl-auto=validate`와 health 200을 확인했다. 252 가격행이 모두 PROVISIONAL인 상태에서 차트 GET은 HTTP 404/S002였다.
- 이 격리 요청은 API가 PROVISIONAL 가격을 FINAL 차트 데이터로 노출하지 않는 동작을 확인한다. 운영 배포 후 검증 시에는 API·Batch 이미지 digest, 구성 checksum, 스키마 버전을 기록하고 실제 배포 대상 API를 격리 DB에 연결해 동일 GET을 확인해야 한다. 현재 운영 API·Batch 이미지는 변경하지 않았다.
- 화면 확인은 해당 FE `StockDetail`의 로딩·빈 상태·오류·모바일 폭에서 수행하고, API 응답 날짜·값·EOD 표시가 같은지 기록한다. 실제 운영 배포와 운영 UI는 확인하지 않았다.

## 테스트 증거

- `NewMarketDataBatchConfigIntegrationTest`는 PostgreSQL 15 Testcontainers에서 252 세션 저장, ticker 범위, 캡처 숫자 대사, 재실행 revision 불변을 확인한다.
- 실제 KIS 격리 실행에서는 원시 가격 252행, 조정 가격 252행, 투자자 3,276행을 저장하고 배치가 소비한 동일 HTTP 응답 본문으로 직접 대사했다. 원시 가격 1,512 필드값, 조정 가격 1,008 필드값, 투자자 3,276 날짜/유형별 수량·금액 쌍에서 차이 0이었다. 격리 fixture에 active canonical `index_series`가 없어 지수 단계는 빈 입력으로 완료됐으며 KIS 지수 endpoint는 호출되지 않았다.
- `./gradlew clean build --no-build-cache --console=plain` 전체 빌드는 통과했다(241 test suites, 959 tests, 실패·오류·skip 0). `KisMarketDataResponseCaptureInterceptorTest`는 허용 경로·비활성·캡처 실패 처리를 확인했고 새 인터셉터 줄 커버리지는 84.4%다. 이번 격리 API 실행은 `ddl-auto=validate`와 health 200으로 기동했고, 실제 KIS 252일 적재 후 차트 GET의 HTTP 404/S002를 확인했다. `git diff --check`도 통과했다.
- 실제 실행 범위의 모든 원천 가격·수급 행은 `PROVISIONAL`이었다. 격리 API의 FINAL 전용 읽기 경로는 확인했지만, 공급자 FINAL 승격, FINAL 데이터가 포함된 성공 차트 응답, 운영 API/화면, 운영 배포는 확인되지 않았다.
- `MarketDataBatchOrchestratorTest`는 ticker 실행이 종목 마스터/전체 파생 잡을 건너뛰고 지정 종목을 가격·투자자 잡에 전달하는지 확인한다.
- `BatchAdminControllerTest`는 관리 요청의 ticker 전달과 비KR ticker 거부를 확인한다.
- `NewMarketDataSyncServiceTest.investor_flows_are_saved_per_type_with_signed_values_and_missing_values_left_null`는 투자자 금액의 백만원→원 환산과 누락값/PROVISIONAL 처리를 확인한다.
