# KIS 계약 근거 재확인 — 2026-11-15 (2026-11-26 업데이트)

범위: 공개 KIS Developers API 목록, KIS 소유 `open-trading-api` 예제, KIS API 전체 문서 내보내기 자료를 확인했다. 인증 호출은 하지 않았다. 내보내기 자료는 `한국투자증권_오픈API_전체문서_20260211_030000`의 읽기 전용 사본이다.

2026-11-16 추가 확인: 아래 「실 KIS 응답·대사 관측」은 격리 컨테이너에서 수행한 별도 인증 실호출 결과다. 앞 문장의 문서 검토 범위에는 포함되지 않는다.

## KIS 공식 변경 공지 확인 — 2026-11-16

- 2026-10-29 공지의 적용 필드는 국내 계좌 권리 조회와 국내·해외 선물옵션 계좌 응답의 계좌번호·계좌상품코드 등 민감정보다. 대상 시장 데이터 endpoint `FHKST03010100`, `FHPTJ04160001`, `FHKUP03500100`은 공지에 없으므로 해당 변경 공지는 본 문서의 가격·투자자 단위나 FINAL 판정 근거를 바꾸지 않는다. [KIS 공식 API 결과값 제공 변경 공지](https://apiportal.koreainvestment.com/community/10000000-0000-0011-0000-000000000001/post/a075a300-09ed-4a79-8801-915d2bb468f6)

## 확인된 계약

- 종목 일봉 `FHKST03010100`의 KIS 전체 문서 탭 `국내주식기간별시세(일_주_월_년)`은 `A14:B14`에서 호출당 최대 100행을 명시한다. `B35:G35`는 이 endpoint의 `FID_ORG_ADJ_PRC`를 `0=수정주가`, `1=원주가`로 설명한다. `output2`의 거래일·OHLC·`acml_vol`·`acml_tr_pbmn` 필드 정의(`B75:G81`)는 단위·정밀도·반올림·수정주가 산식을 기재하지 않는다. `G84`의 `mod_yn` 설명은 당일 체결이 없어 시가가 없을 때 차트에서 `Y`를 표시한다고 정의한다. 이는 차트 표시 의미이며 FINAL 표식이나 `N=확정` 근거가 아니다. 같은 이름의 인자는 별도 `inquire_daily_price` endpoint에서 반대 의미로 설명되므로 endpoint별 계약을 적용한다. [KIS 전체 API 문서 사본](https://docs.google.com/spreadsheets/d/1mDOe3herlalfgcmlxrdp-fXPf2Ck6VWVHWlx4Ur983o/edit), [일봉 호출 예제](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/inquire_daily_itemchartprice.py), [일봉 필드 매핑](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/chk_inquire_daily_itemchartprice.py), [별도 daily-price endpoint](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_daily_price/inquire_daily_price.py)
- 종목 투자자 일별 `FHPTJ04160001`은 거래일을 요청하고 `output1`·`output2`를 읽는다. 전체 문서 내보내기의 `종목별 투자자매매동향(일별)` 탭은 `A14:B14`에서 이 endpoint 단위를 `금액(백만원), 수량(주)`로 명시하고, `G56:G57`은 `acml_vol`을 주, `acml_tr_pbmn`을 백만원으로 표시한다. 따라서 해당 endpoint의 금액 필드에 대한 `×1,000,000` 원화 환산과 수량의 주 단위는 근거가 있다. 같은 필드명이라도 이 근거를 `FHKST03010100`을 포함한 다른 endpoint로 확장하지 않는다. 예제의 표시용 변환·반올림은 공급자 저장 정밀도 계약이 아니다. 장 마감 후 조회 가능 설명은 제공 시점이며 FINAL 규칙은 아니다. [KIS 전체 API 문서 사본](https://docs.google.com/spreadsheets/d/1mDOe3herlalfgcmlxrdp-fXPf2Ck6VWVHWlx4Ur983o/edit), [일별 요청 예제](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/investor_trade_by_stock_daily/investor_trade_by_stock_daily.py), [일별 필드 매핑](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/investor_trade_by_stock_daily/chk_investor_trade_by_stock_daily.py)
- 국내 지수 일봉 `FHKUP03500100` 예제는 국내 지수 경로와 OHLC 필드명을 확인해 준다. 지수·수급·거래대금 필드의 단위, 정밀도, FINAL 조건은 해당 공개 예제에서 확인되지 않았다. 해외 지수 예제는 미국 지수 조회 지원 대상을 Dow 30·Nasdaq 100·S&P 500으로 설명하고 `mod_yn`을 “변경 여부”로 매핑한다. [국내 지수 호출·필드 예제](https://github.com/koreainvestment/open-trading-api/blob/main/backtester/kis_backtest/providers/kis/data.py), [해외 지수 필드 매핑·지원 범위](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/overseas_stock/inquire_daily_chartprice/chk_inquire_daily_chartprice.py)
- 공개 API 목록은 endpoint 이름·분류를 제공한다. 검토한 목록과 KIS 소유 예제에서는 FINAL 상태 필드, 확정 시각, `mod_yn` 값과 수정 이력의 관계, 정정 보존 기간을 찾지 못했다. [KIS Developers API 목록](https://apiportal.koreainvestment.com/apiservice-summary)

## KIS 원본 문서 셀 재확인 — 2026-11-16

- 연결된 Google Sheets 원본 메타데이터의 문서명은 `한국투자증권_오픈API_전체문서_20260211_030000`이다. 정확한 탭 `국내주식기간별시세(일_주_월_년)`에서 `A14:G14`, `B35:G35`, `B75:G81`, `G84`를 읽었다. 이는 호출당 최대 100건, `FID_ORG_ADJ_PRC`의 `0=수정주가/1=원주가`, OHLC·거래량·거래대금 필드명/타입·길이, 그리고 체결이 없을 때 차트에서 사용하는 `mod_yn=Y` 설명을 확인한다. 해당 필드 정의에는 가격·거래량·거래대금 단위, 저장 정밀도·반올림, 수정주가 산식이 없다.
- 정확한 탭 `종목별 투자자매매동향(일별)`의 `A14:B14`, `G56:G57`을 읽었다. 개요는 금액(백만원)·수량(주)을 명시하며 예시 필드도 각각 주와 백만원 단위를 표시한다.
- 두 endpoint 탭의 현재 그리드 `A1:Z1000`을 대상으로 `FINAL`, `확정`, `최종`, `잠정`을 각각 검색했으며 일치 행은 없었다. 이는 이 문서 사본과 두 탭 안에서의 검색 결과이며, KIS 내부 기준이나 이후 갱신된 비공개 자료의 부재를 증명하지 않는다. 문서 메타데이터의 제목은 2026-02-11 export를 가리키므로 더 최신 계약 버전 여부도 확인되지 않았다. [KIS API 전체 문서](https://docs.google.com/spreadsheets/d/1mDOe3herlalfgcmlxrdp-fXPf2Ck6VWVHWlx4Ur983o/edit)

## KIS Developers 현재 endpoint 문서 재확인 — 2026-11-16

KIS Developers 공개 API 목록의 endpoint 상세 레코드를 확인했다. 레코드의 수정 일자는 가격 `2026-03-27T13:01:20+09:00`, 투자자 `2026-04-10T20:48:57+09:00`이다. 아래 내용은 해당 시점에 공개된 문서 근거다.

- 가격 `FHKST03010100` (`국내주식기간별시세(일/주/월/년)[v1_국내주식-016]`, record `a08c3421-e50f-4f24-b1fe-64c12f723c77`)는 회당 최대 100건과 `FID_ORG_ADJ_PRC`의 `0=수정주가`, `1=원주가`를 명시한다. `output2`의 OHLC, `acml_vol`, `acml_tr_pbmn` 필드 문서에는 단위 설명이 없다. `mod_yn`은 당일 체결이 없어 시가가 없을 때 차트에 쓰는 `Y` 표기다. `FINAL` 상태/시각 규칙은 요약 및 필드 설명에 없다. [KIS 가격 endpoint 원문 레코드](https://apiportal.koreainvestment.com/api/apis/public/api-list/1031d032-6747-414b-bdf2-fd419c0c98ce)
- 투자자 `FHPTJ04160001` (`종목별 투자자매매동향(일별)`, record `141f0f11-ef30-4e52-9388-a6e78502d5c2`)는 요약에 금액 백만원·수량 주를 명시하고, 당일 값은 15:40 이후 가집계되어 조회 가능하며 산출 시각이 일정하지 않을 수 있다고 안내한다. 필드 설명도 `acml_vol`을 주, `acml_tr_pbmn`을 백만원으로 명시한다. 이는 수집·저장 단위 근거이지 역사적 값의 FINAL 여부를 입증하지 않는다. `FINAL` 상태/시각 규칙은 요약 및 필드 설명에 없다. [KIS 투자자 endpoint 원문 레코드](https://apiportal.koreainvestment.com/api/apis/public/api-list/4e29f215-0a20-4396-95d4-5d99ad9a2c3d)
- 두 endpoint의 공개 요약과 요청·응답 필드명/설명에서 `FINAL`, `확정`, `최종`, `잠정`을 검색했으며 일치 항목이 없었다. 검색 범위 밖의 KIS 내부·비공개 기준, 고객별 공지, 또는 이후 변경 가능성까지 부정하지 않는다.

## 관측과 해석

기존 비밀 제거 응답 기록에는 같은 종목·날짜·요청 조건의 성공 응답에서 종가가 `206500`에서 다음 조회 `208000`으로 바뀌었고 두 응답 모두 `mod_yn=N`인 사례가 있다. 이는 성공 응답·장 마감·`mod_yn=N`만으로 불변 또는 FINAL을 판정할 수 없다는 관측이다. 최종 확정 시점을 이 사례로 추정하지 않는다. 세부 캡처 범위는 [기존 계약 기록](2026-11-13-kis-source-contract.md)을 참조한다.

## 단위 근거 범위

| 데이터 | 확인된 근거 | 남은 공백 |
|---|---|---|
| 종목 일봉 `FHKST03010100` | 2026-03-27 수정 KIS Developers 레코드와 문서 사본: 최대 100행, 조정 플래그, 차트용 `mod_yn` 의미 | `mod_yn`은 FINAL을 뜻하지 않음; OHLC·거래량·거래대금의 단위·정밀도와 수정주가 산식 |
| 투자자 일별 `FHPTJ04160001` | 2026-04-10 수정 KIS Developers 레코드와 문서 사본: 금액 백만원, 수량 주; 당일 가집계 안내 | FINAL 규칙, 필드별 반올림·공급자 저장 정밀도 |
| 국내 지수·업종 | endpoint·필드 이름과 일부 조회 상한 | 저장 단위·정밀도·수정 기준·FINAL 규칙 |
| 해외 지수 | 지원 인덱스와 일부 decimal 자릿수 표기 | 자릿수의 단위·저장 변환 의미·FINAL 규칙 |

## 실행 판정

- 공급자 FINAL 근거가 확보될 때까지 모든 원천 행은 `PROVISIONAL`로 유지하고 FINAL 기반 사용자·파생 조회에서 제외한다.
- 투자자 일별 `FHPTJ04160001`의 금액 백만원·수량 주 단위는 문서 사본과 2026-04-10 수정 KIS Developers 레코드로 확인했다. 현재 코드의 해당 endpoint 금액 `×1,000,000` 변환은 이 단위와 일치한다. 가격·거래대금·지수·업종의 단위 및 정밀도는 endpoint별 문서 근거나 KIS 서면 확인 전까지 미확정이다.
- 원본 응답과 공급자 문자열을 보존하는 검증은 계속할 수 있다. 근거가 없는 단위 변환·FINAL 승격은 인수 근거로 사용하지 않는다.
- 이 재확인은 비밀 없는 KIS 문서 사본과 공개 예제를 다뤘다. FINAL 근거와 나머지 endpoint의 단위·정밀도 확인 없이는 #320의 FINAL·단위 인수를 완료할 수 없다.

## 실 KIS 응답·대사 관측 — 2026-11-16

- 격리 PostgreSQL 15의 `005930` 대상으로 `2025-11-04..2026-11-13` KOSPI 252 세션을 수집했다. 가격 저장은 252일/252행, 투자자 저장은 252일×13 유형=3,276행이었다. 누락·불완전 행은 0이고 모든 행은 `PROVISIONAL`이었다.
- `FHPTJ04160001`을 기준일별로 조회하면 `output2`에 기준일과 그 이전 거래일이 함께 포함됐다. 최대 30행 응답이 관측됐으며, 252개 세션에 대해 9개 기준일을 조회한 재대사 캡처는 270행(252개 대상 날짜와 18개 중복 과거 행)이었다. 이 동작은 실호출 관측이며 문서에 명시된 계약으로 확대 해석하지 않는다. 범위 적재는 30 세션 단위 조회 후 각 응답에서 요청 날짜만 저장한다.
- 배치 적재 후 KIS를 별도로 재조회해 숫자를 대사했다. 가격 `open/high/low/close`, `acml_vol`, `acml_tr_pbmn` 비교 차이는 각각 0건이었다. 투자자 3,276 날짜/유형 키의 수량과 백만원→원 금액 비교도 차이 0건이었다. 가격 필드 단위는 여전히 미확정이며, 대사에서 임의 환산하지 않고 응답 숫자를 그대로 비교했다.
- 분리된 두 번째 252일 적재 후 가격과 투자자 row 모두 revision 1을 유지했다. 이는 현재 응답 값에 대한 멱등 재수집 관측이며, 향후 공급자 정정 처리를 보장하는 근거는 아니다.
- 이전 별도 원천 재조회 파일의 SHA-256은 `5fce418a9696e60c57ae896242ebacdda3ea5a7627486ed13d188214e5a4f6d0`이다. 이는 배치 HTTP 응답과 같은 본문이 아니며, 아래의 동일 응답 캡처 검증으로 원본 대사 증거가 보완됐다.
- `mod_yn`은 이전 문서 설명과 같이 FINAL을 뜻하지 않는다. 실제 적재의 252일 모두 `PROVISIONAL`이므로 KIS FINAL 규칙, 확정 시각, FINAL 승격은 미확정이다.

## 동일 HTTP 응답 캡처와 PostgreSQL 대사 — 2026-11-16

### 검증 범위

- 격리 PostgreSQL 15, 종목 `005930`, KOSPI 달력의 열린 세션 `2025-11-04..2026-11-13` 252일을 대상으로 실제 관리 endpoint `POST /api/v1/admin/batch/market-data/sync`를 두 번 호출했다. 두 번째 요청은 별도 `runId`를 사용했다. 두 번째 HTTP 응답은 200/S000이었고 가격·투자자·지수 단계가 모두 `COMPLETED`였다.
- 선택 캡처 설정으로 `FHKST03010100` 종목 일봉 가격과 `FHPTJ04160001` 종목별 투자자 일봉의 실제 HTTP 응답 body를 DTO 파싱 전에 저장했다. 한 실행당 17개 요청 조합(가격 4개 날짜 범위×원시/조정 플래그, 투자자 9개 기준일)을 캡처했고, 두 실행 총 34개 body와 메타데이터를 검증했다. 각 metadata SHA-256이 body 파일 SHA-256과 일치했으며 동일 요청 조합의 두 실행 body 해시는 17/17개 모두 동일했다. 원본 파일은 대사 후 격리 임시 디렉터리와 함께 정리했다.

### 캡처 본문 SHA-256

가격 endpoint의 네 날짜 범위에서 원시 플래그(`FID_ORG_ADJ_PRC=1`)와 조정 플래그(`=0`) 응답 body는 각 범위마다 같은 SHA-256이었다. 이는 이 실행의 관측이며 두 플래그의 계약상 의미가 같다는 근거는 아니다.

| 가격 날짜 범위 | 원시·조정 응답 body SHA-256 |
|---|---|
| `2025-11-04..2026-02-11` | `151ef301bb165eea63cd82dd4879b28cffed5e072b8b46aa4aaa8ce29c497f86` |
| `2026-02-12..2026-05-22` | `8ab988546640ec6400119af19d964d94c13e3812318378d69c4966d76843286a` |
| `2026-05-26..2026-08-28` | `1cee81bcabc306c98cb8397470977b72fe3487de83a37f5d1eaa8e8a742392b8` |
| `2026-08-31..2026-11-13` | `f47171b181b50652c91d99fe83bd3b63512b28f9de05b4b3aee531e61104b3af` |

투자자 endpoint의 기준일별 응답 body SHA-256:

| 요청 기준일 | 응답 body SHA-256 |
|---|---|
| `2025-12-15` | `41ca123f5035c088ac8f5463d6fc023ef80672f34018875ff0b8f78176217330` |
| `2026-01-29` | `9752f09a61aa7dad8625a9ef7149a7a5f9f568fbe567120c0db6d874b2d36440` |
| `2026-03-18` | `cb1321874d3b28abaac3f20252aed5c48640057f45d7a08da6fc5141483a8a48` |
| `2026-04-29` | `c602f83adf4e60166f0c68e66437adbb402a6f94dabd682a1ee82c9b3f70e77b` |
| `2026-06-16` | `a1cdfef170de09565fd1fa17bb94f3d714b9815372da9981136e6a07dbb36bbc` |
| `2026-07-29` | `906da2de2628e7d145c0bd3954145caa70b005db91f74a64455ff80191f83a37` |
| `2026-09-10` | `7261917e7bf7037f159a02f490ec76e20895b98e3113f001ed6f4d697ca46c90` |
| `2026-10-28` | `935408409f69ff1b152dbcff7641d283ea4881f201bc01b156fe074e7a62caca` |
| `2026-11-13` | `1ad782a7581c3390fa11923ea46d7be14b88823940dcb865dff073b6c3bdb97d` |

### PostgreSQL 비교 결과

- 원시 일봉 `stock_price_daily`: 252개 거래일×6개 값(OHLC, 거래량, 거래대금), 총 1,512개 값을 저장 직후 소비한 원시 응답과 비교했다. 숫자 차이 0건이다. 가격·거래량·거래대금은 단위 근거가 미확정이므로 원천 숫자를 임의 변환하지 않고 대사했다.
- 조정 일봉 `stock_price_adjustment`: 252개 거래일×4개 값(OHLC), 총 1,008개 값을 해당 조정 플래그 응답과 비교했다. 차이 0건이다. 위에서 기록한 원시/조정 body 동일성은 응답 관측으로만 취급하고 조정 가격 계약을 확정하지 않는다.
- 투자자 `stock_investor_trade_daily`: 252일×13 유형=3,276 날짜/유형 키, 수량과 금액 두 값씩 총 6,552개 값을 비교했다. 수량은 응답값 그대로, 금액은 공식 `FHPTJ04160001` 문서의 백만원 단위에 따라 `×1,000,000` 변환해 차이 0건이다. 마지막 12세션 chunk 요청의 응답은 30행을 반환했으며, 서비스가 요청 chunk에 속하는 12일만 저장하는 동작도 확인했다.
- 두 실행 후 원시 가격 252행 및 투자자 3,276행의 revision은 모두 1이었고 `INCOMPLETE`는 0이었다. 가격·수급은 모두 `PROVISIONAL`이다. 이 검증은 두 실행 시점의 같은 응답을 확인한 것이며 향후 공급자 정정이나 FINAL을 보장하지 않는다.
- 격리 fixture에는 active canonical `index_series`가 0개라 지수 단계는 빈 입력으로 완료됐고 `FHKUP03500100` 요청은 발생하지 않았다. 지수·업종의 이번 실행 원본 대사는 N/A이며 단위/정밀도/FINAL은 여전히 미확정이다. 검증 종목도 `005930` 한 종목뿐이다.

### 격리 API 읽기 경로 — 2026-11-16

- 같은 252일을 적재한 격리 PostgreSQL에 API jar(`b89c04edcb3d9166de987617b6614ab36ec8f647057bd956edadfc9b1414f234`)를 `prod` 프로파일로 연결했다. 테스트용 통합 fixture에서 `ddl-auto=validate`와 Actuator health HTTP 200을 확인했다.
- 이번 격리 실행의 배치 POST는 HTTP 200/S000, 26/26 stage `COMPLETED`였다. DB에서 열린 세션 252개, `stock_price_daily` 252행/252일, `stock_price_adjustment` 252행, `stock_investor_trade_daily` 3,276행/252일을 확인했다. 가격·투자자 행은 모두 `PROVISIONAL`, `FINAL`과 `INCOMPLETE`는 0, revision 최대값은 1이었다.
- 실제 `GET /api/v1/stocks/005930/prices/history?period=1Y&frequency=DAILY` 요청은 HTTP 404/S002, `해당 기간의 시세 데이터를 찾을 수 없습니다.`를 반환했다. 해당 DB의 252 가격행은 모두 `PROVISIONAL`; `StockPriceReadAdapter`가 `data_status = 'FINAL'`만 읽어 이 결과는 현재 계약에 부합한다.
- 이 결과는 격리된 API → PostgreSQL 실제 요청 경로의 검증이다. 운영 API 배포, 운영 DB 적재/전환, FINAL 가격이 있는 성공 응답, 전체 종목 표본, 지수 endpoint, FE 화면은 검증하지 않았다.

앞선 원본 대사 증거는 동일 Batch → KIS → 격리 PostgreSQL 요청 경로에 한정된다. KIS FINAL 근거와 운영 스키마 호환성이 미확정인 동안 운영 배포는 보류한다.

## 엔드포인트별 요청 계약 요약 (2026-11-26 통합)

| TR ID / 구분 | 요청 경로 | 필수 쿼리 파라미터 예시 | 회당 조회 상한 (근거 vs 관측) | 지원 범위 | 저장소 비밀 제거 실응답 fixture |
|---|---|---|---|---|---|
| `FHKST03010100` (RAW) | `/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice` | `FID_COND_MRKT_DIV_CODE=J`, `FID_INPUT_ISCD=005930`, `FID_INPUT_DATE_1=20261112`, `FID_INPUT_DATE_2=20261112`, `FID_PERIOD_DIV_CODE=D`, `FID_ORG_ADJ_PRC=1` | **100건** (문서 근거: `20260211` 사본 `A14:B14`, 2026-03-27 `a08c3421...`). 코드는 100 calendar-day 창 분할(`STOCK_MAX_ROWS=100`). | `ACTIVE`, `KOSPI`/`KOSDAQ`, `ST`/`EF`/`EN`, `000000..999999`, `listing_date` 이후 세션 | 있음 (`domestic-daily-success.json`, SHA-256 `fa0178a6...`) |
| `FHKST03010100` (ADJUSTED) | `/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice` | 위와 동일 + `FID_ORG_ADJ_PRC=0` | **100건** (문서 근거 동일, `FID_ORG_ADJ_PRC` `0=수정주가`/`1=원주가`는 이 endpoint 한정). | 위와 동일 (`stock_price_adjustment`, `PROVIDER_ADJUSTED`) | 미보관 (2026-11-16 대사 시 RAW/ADJUSTED body SHA-256 동일 관측 기록만 존재) |
| `FHPTJ04160001` | `/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily` | `FID_COND_MRKT_DIV_CODE=J`, `FID_INPUT_ISCD=005930`, `FID_INPUT_DATE_1=20261113`, `FID_ORG_ADJ_PRC=""`, `FID_ETC_CLS_CODE=""` (`custtype=P`) | 최대 **30거래일** 반환은 2026-11-16 실호출 관측(공식 상한 문서 계약 여부 미확정). 코드는 30세션 창 분할(`INVESTOR_MAX_ROWS=30`) 후 창 외 과거 행 무시. | 비-상장폐지 `KOSPI`/`KOSDAQ` 종목 × 13개 투자자 유형 (`InvestorSourceType`) | 미보관 (`KisDailyPriceAdapterTest`는 합성 JSON 사용) |
| `FHKUP03500100` | `/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice` | `FID_COND_MRKT_DIV_CODE=U`, `FID_INPUT_ISCD=0001`, `FID_INPUT_DATE_1=20261112`, `FID_INPUT_DATE_2=20261112`, `FID_PERIOD_DIV_CODE=D` (`custtype=P`) | 코드는 보수적으로 **50 calendar days** 창 분할(`INDEX_MAX_ROWS=50`). 공식 회당 최대 건수 미확정. | 국내 시장지수 4종(`0001`, `1001`, `2001`, `3003`) + 검증 업종 67종 (`PRICE`/`NONE`/`KRW`) | 미보관 (`#331` `2026-11-17 12:03 KST` 댓글 기록 기준으로는 국내 지수 71 series의 252-date coverage만 확인되고 행 값 대사가 남아 있었음. 별도 후속 외부 기록인 `2026-11-17 17:33 KST` PO 읽기 전용 보고서 `stockwellness-project/docs/qa/issue-331-handoff/2026-11-17-task-1-read-only-reproduction.md`가 격리 PostgreSQL `stockwellness-acceptance-pg-20261116`의 71 series `2025-11-04..2026-11-13` 17,892행과 별도 작업트리에 보존된 568개 원문 응답을 오프라인 대사해 OHLCV·거래대금 불일치 0, `PROVISIONAL` revision 1(17,854행)/revision 2(38행) 일치를 기록함. 다만 38행 원인 조사는 범위 제외, `FINAL`·단위 근거 미확정, 본 저장소 비밀 제거 fixture 미적재 상태는 유지됨) |
| `FHKST03030100` | `/uapi/overseas-price/v1/quotations/inquire-daily-chartprice` | `FID_COND_MRKT_DIV_CODE=N`, `FID_INPUT_ISCD=SPX`, `FID_INPUT_DATE_1=20261112`, `FID_INPUT_DATE_2=20261112`, `FID_PERIOD_DIV_CODE=D` (`custtype=P`) | 코드는 보수적으로 **50 calendar days** 창 분할(`INDEX_MAX_ROWS=50`). 공식 회당 최대 건수 미확정. | US 3종(`SPX` NYSE, `NDX` NASDAQ, `.DJI` NYSE, `USD`). `COMP`/`SOX` fail-closed 거부. | 있음 (`overseas-index-daily-2026-11-12.json`, 3종 샘플; `#331` `2026-11-17 12:03 KST` 기록에 US 3종 보존 캡처 33쌍·756행 오프라인 대사 차이 0 포함, 단위·FINAL 미확정 유지) |
| `CTCA0903R` | `/uapi/domestic-stock/v1/quotations/chk-holiday` | `BASS_DT=20261119`, `CTX_AREA_NK=""`, `CTX_AREA_FK=""` (`custtype=P`) | 일별 개장 여부(`opnd_yn` Y/N)만 제공, 세션 시각 미제공. | KRX `KOSPI`/`KOSDAQ` 쌍, 2025–2026 검증 연도 기준(요청 `baseDate`의 미검증 연도 영업일 `opnd_yn=Y`만 fail-closed; 검증 연도 요청의 `2027+` lookahead 행은 저장 제외) | 미보관 (세션 시각 검증용 `market-sessions-2026.json`만 보유) |

## 필드별 단위·정밀도·DB 매핑·근거 상태 (2026-11-26 통합)

| Endpoint / TR | 응답 필드 (`output2`) | DB 테이블 / 컬럼 (`02-schema.sql`) | 코드 변환 (`NewMarketDataSyncService`) | 근거 문서 / 레코드 | 상태 |
|---|---|---|---|---|---|
| `FHKST03010100` (RAW) | `stck_oprc`, `stck_hgpr`, `stck_lwpr`, `stck_clpr` | `stock_price_daily`: `open_price`, `high_price`, `low_price`, `close_price` (`numeric(19,4)`) | 원문 그대로 `scale=4, RoundingMode.UNNECESSARY` | `20260211` `B75:G81`, `a08c3421...` (단위·정밀도 미기재) | `UNCONFIRMED(BLOCKED)` |
| `FHKST03010100` (RAW) | `acml_vol`, `acml_tr_pbmn` | `stock_price_daily`: `volume` (`bigint`), `trading_amount` (`numeric(25,2)`) | 원문 그대로 `Long` / `scale=2, UNNECESSARY` (임의 환산 금지) | 위와 동일 (단위·정밀도 미기재) | `UNCONFIRMED(BLOCKED)` |
| `FHKST03010100` (RAW) | `mod_yn`, `rt_cd` | `stock_price_daily`: `data_status` (`PROVISIONAL`/`INCOMPLETE`), `revision` | `PROVISIONAL` 유지 (`FINAL` 승격 금지) | `20260211` `G84` (체결 부재 차트 표기) | `CONFIRMED`(비-FINAL) / `BLOCKED`(FINAL 규칙) |
| `FHKST03010100` (ADJUSTED) | `stck_oprc`, `stck_hgpr`, `stck_lwpr`, `stck_clpr` (`FID_ORG_ADJ_PRC=0`) | `stock_price_adjustment`: `open_price`, `high_price`, `low_price`, `adjusted_close` (`numeric(19,4)`, `PROVIDER_ADJUSTED`) | 원문 그대로 `scale=4, UNNECESSARY` | `20260211` `B35:G35` (`0=수정주가`, 산식 미기재) | `UNCONFIRMED(BLOCKED)` |
| `FHPTJ04160001` | 13개 유형 `*_ntby_qty` / `*_ntby_vol` | `stock_investor_trade_daily`: `net_quantity` (`numeric(19,6)`) | 원문 그대로 `scale=6, UNNECESSARY` | `20260211` `A14:B14`, `G56`; `141f0f11...` (2026-04-10): **주** | 단위 `CONFIRMED` / 반올림·정밀도 `BLOCKED` |
| `FHPTJ04160001` | 13개 유형 `*_ntby_tr_pbmn` | `stock_investor_trade_daily`: `net_amount` (`numeric(25,2)`) | `×1,000,000` (백만원→원), `scale=2, UNNECESSARY` | `20260211` `A14:B14`, `G57`; `141f0f11...` (2026-04-10): **백만원** | 단위 `CONFIRMED` (`FHPTJ04160001` 한정) / 반올림·FINAL `BLOCKED` |
| `FHKUP03500100` | `bstp_nmix_oprc`, `bstp_nmix_hgpr`, `bstp_nmix_lwpr`, `bstp_nmix_prpr` | `index_daily`: `open_value`, `high_value`, `low_value`, `close_value` (`numeric(19,4)`) | 원문 그대로 `scale=4, UNNECESSARY` | 공개 예제 필드 매핑만 존재 (단위·정밀도 미기재) | `UNCONFIRMED(BLOCKED)` |
| `FHKUP03500100` | `acml_vol`, `acml_tr_pbmn` | `index_daily`: `volume` (`bigint`), `trading_amount` (`numeric(25,2)`) | 원문 그대로 `Long` / `scale=2, UNNECESSARY` | 단위(주 vs 천주, 원 vs 백만원) 미확인 | `UNCONFIRMED(BLOCKED)` |
| `FHKST03030100` | `ovrs_nmix_oprc`, `ovrs_nmix_hgpr`, `ovrs_nmix_lwpr`, `ovrs_nmix_prpr` | `index_daily`: `open_value`, `high_value`, `low_value`, `close_value` (`numeric(19,4)`) | 원문 그대로 `scale=4, UNNECESSARY` | 해외 지수 예제 및 2026-11-13 캡처 3종 (4자리 소수 반올림 규칙 미확인) | `UNCONFIRMED(BLOCKED)` |
| `FHKST03030100` | `acml_vol`, 미제공 `trading_amount` | `index_daily`: `volume` (`bigint`), `trading_amount` (`null`) | `SPX`/`NDX` `0` 보존, `.DJI` 정수 보존, `trading_amount=null` | `overseas-index-daily-2026-11-12.json` | `UNCONFIRMED(BLOCKED)` (`acml_vol` 의미·단위) |
| `CTCA0903R` + 거래소 공지 | `bass_dt`, `opnd_yn` | `trading_calendar`: `is_trading_day`, `opens_at`, `closes_at` (`timestamptz`) | KRX `2025–2026` 공식 시각 결합; US `2025–2027` (`UsMarketCalendar`) 결합 | `krx-session-calendar.md`, `us-session-calendar.md` | KRX `2025–2026`·US `2025–2027` `CONFIRMED` / KRX `2027+` `BLOCKED` |

## 공식 KIS 확인 질의 체크리스트 (Blocked-1 ~ Blocked-2 해제 조건)

1. `FHKST03010100`, `FHPTJ04160001`, `FHKUP03500100`, `FHKST03030100` 각 TR에 장 마감 후 확정(`FINAL`) 상태를 판별할 수 있는 공식 응답 필드/코드가 존재하는지 여부.
2. 별도 확정 필드가 없다면 각 endpoint별 공식 EOD 확정 기준 시각(KRX 정규장/수능일, US 정규장/조기종료일)과 사후 정정 가능 기간.
3. `2026-11-12` `005930` 동일 조건 재조회 시 `rt_cd=0`, `mod_yn=N` 상태에서 종가가 `206500 → 208000`으로 변동한 공식 원인(시간외/NXT 반영 또는 장 마감 후 정정 등).
4. `FHKST03010100`의 OHLC·`acml_vol`·`acml_tr_pbmn` 공식 단위·소수 정밀도·반올림 규칙 및 `FID_ORG_ADJ_PRC=0` 수정주가 산식(배당·분할·감자 소급 기준).
5. `FHPTJ04160001`의 반올림·절사·공급자 저장 정밀도 규칙 및 1회 최대 30거래일 반환의 공식 계약 여부.
6. `FHKUP03500100`의 지수·`acml_vol`(주 vs 천주)·`acml_tr_pbmn`(원 vs 백만원) 공식 단위/회당 최대 건수 및 `index_daily` breadth·지수 수급 원천 TR; `FHKST03030100`의 `ovrs_nmix_*` 반올림·`acml_vol` 의미/단위·회당 최대 건수 및 2026-12-06 Nasdaq 23/5 도입 이후 일봉 정규장(16:00 ET) 기준 유지 여부.

## 이슈 #320 상태 및 2026-11-26 코드·회귀 보강 내역

- **이슈 #320 상태:** **`OPEN` 유지** (#320 AC #5). 공급자 `FINAL` 판정 규칙(Blocked-1)과 가격·지수·업종 단위/정밀도·조정 산식(Blocked-2) 미확정으로 승격·단위 확정 구현은 보류한다.
- **2026-11-26 검증 완료 범위 (`READY`):**
  - `NewMarketDataSyncService`의 지수·주식·투자자 수집 경로 전반에 주입된 `Clock`을 일관 적용해(`adjustmentAsOf` 및 투자자 `collectedAt` 포함) 동일 요청 내 날짜 검증(`ensureEndDateNotFuture`)과 당일 `trading_calendar.closes_at` 완료 판정(`completedOpenDates`)이 동일한 `Instant`를 사용하도록 정합화했다. 요청 범위 내 대상 영업일(종목의 경우 `listing_date` 이후 영업일) 중 **어느 하나라도 미완료이거나 당일 `closes_at`이 누락되면** 외부 KIS API 호출이나 DB `upsert` 전에 `IllegalStateException`으로 fail-closed 실패해 부분 적재 후 재실행 누락을 방지한다. 신규상장 이전 빈 범위와 휴장일 전용 범위는 KIS 호출·저장 없이 정상 종료한다.
  - `NewMarketDataSyncService.syncTradingCalendar`에서 검증된 연도(`2025–2026`) `baseDate` 요청 응답에 포함된 미검증 미래 연도 lookahead 행(`> 2026`, `!= baseDate`)은 형식·중복 검증을 유지한 채 저장 대상에서 제외해 연말 `CTCA0903R` 다일 응답이 현재 연도 달력 적재를 막지 않도록 수정했다(미검증 연도 영업일 `opnd_yn=Y`에 한해 fail-closed 유지).
  - `MarketDataBatchOrchestrator.runScheduled("US", ...)`가 연도 경계(예: `2027-01-04T20:59:59Z` → `latest=2026-12-31`, `2027-01-01T21:15:00Z` 휴장일 크론)에서도 직전 완료 세션을 달력 준비 범위에 포함하도록 보강하고, KRX `syncTradingCalendar`의 정규장·연초 개장일·수능일·휴장일·미검증 연도/오류 응답 fail-closed 단위 테스트를 추가했다.
  - 4개 기간별 API(`FHKST03010100`, `FHPTJ04160001`, `FHKUP03500100`, `FHKST03030100`)의 252거래일 수집 창 분할(100일·30세션·50일)과 중복/누락 0·`PROVISIONAL` 유지·과거 재수집 원자성 회귀 테스트를 추가했다.
  - `overseas-index-daily-2026-11-12.json`(`SPX`, `NDX`, `.DJI`) 직접 입력과 `domestic-daily-success.json`(`206500`) + 2026-11-13 재조회(`208000`, `mod_yn=N`) 재현 합성 입력에 대한 `PROVISIONAL` 유지·revision 증가 회귀 테스트를 추가했다.
- **남은 차단 범위 (`BLOCKED`) 및 미검증·후속 과제:**
  - `Blocked-1`(`FINAL` 근거), `Blocked-2`(단위/정밀도/조정 산식), `Blocked-3`(본 저장소 미보관 비밀 제거 실응답 fixture 보완; `#331` `2026-11-17 12:03 KST` 기록의 US 3종 33쌍/756행 대사와 외부 `2026-11-17 17:33 KST` PO 보고서의 국내 지수 71 series/17,892행 대사는 별도 기록이나, 본 저장소 `src/test/resources/kis/source-contract/` fixture 적재·38행 원인 조사·`FINAL`/단위는 미완료 상태 유지), `Blocked-4`(KRX 2027년 공식 달력).
  - **잔여 미검증·운용 위험 (`#320` 외부/후속 과제):**
    1. 레거시 `benchmarkPriceSyncJob`(`BenchmarkPriceBatchConfig`/`BenchmarkPriceDataReader`)은 `NewMarketDataSyncService`를 거치지 않아 `trading_calendar`/`isEodSessionComplete`를 검증하지 않고 `DateUtil.today()` 기본 범위로 조회하며 실패 시 일부 지수만 건너뛰고 계속 진행한다.
    2. `StockInvestorTradeDetailScheduler`는 평일 `16:00 KST` 고정 cron으로 `stockInvestorTradeDetailJob`을 호출하므로 KRX 수능일(16:30 KST 종료)에는 `NewMarketDataSyncService`의 fail-closed 가드에 의해 차단되며 `16:30 KST` 이후 자동 재시도를 수행하지 않는다(정규 스케줄은 `16:45 KST` `StockwellnessScheduler`가 담당).
    3. `stock_price_adjustment` 단독 소급 변동(원시 주가 `stock_price_daily` 불변 상태에서 수정주가만 정정) 시 `EodStockPriceAdapter.writeAdjustments`가 `stock_price_daily.revision`을 증가시키거나 `market-data-revision` Outbox 이벤트를 발행하지 않으므로, 향후 `FINAL` 전환 및 `PROVIDER_ADJUSTED` 지표·캐시 무효화 연계 전 별도 계약 보강이 필요하다.
