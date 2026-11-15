# KIS 계약 근거 재확인 — 2026-11-15

범위: 공개 KIS Developers API 목록, KIS 소유 `open-trading-api` 예제, KIS API 전체 문서 내보내기 자료를 확인했다. 인증 호출은 하지 않았다. 내보내기 자료는 `한국투자증권_오픈API_전체문서_20260211_030000`의 읽기 전용 사본이다.

## 확인된 계약

- 종목 일봉 `FHKST03010100`은 호출당 최대 100행을 제공한다. 이 endpoint의 `FID_ORG_ADJ_PRC`는 `0=수정주가`, `1=원주가`다. KIS 예제는 OHLC·거래일·누적 거래량·누적 거래대금 필드명을 보여주지만 단위, 정밀도, 반올림 또는 수정주가 산식은 정의하지 않는다. 같은 이름의 인자는 다른 `inquire_daily_price` endpoint에서 반대 의미로 설명되므로 endpoint별 계약을 적용해야 한다. [일봉 호출 예제](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/inquire_daily_itemchartprice.py), [일봉 필드 매핑](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_daily_itemchartprice/chk_inquire_daily_itemchartprice.py), [별도 daily-price endpoint](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/inquire_daily_price/inquire_daily_price.py)
- 종목 투자자 일별 `FHPTJ04160001`은 거래일을 요청하고 `output1`·`output2`를 읽는다. 전체 문서 내보내기의 `종목별 투자자매매동향(일별)` 탭은 이 endpoint의 단위를 `금액(백만원), 수량(주)`로 명시한다. 따라서 해당 endpoint의 금액 필드에 대한 `×1,000,000` 원화 환산과 수량의 주 단위는 근거가 있다. 이 근거는 다른 KIS endpoint의 금액 필드까지 확장되지 않는다. 예제의 표시용 변환·반올림은 공급자 저장 정밀도 계약이 아니다. 장 마감 후 조회 가능 설명은 제공 시점이며 FINAL 규칙은 아니다. [KIS 전체 API 문서 사본](https://docs.google.com/spreadsheets/d/1mDOe3herlalfgcmlxrdp-fXPf2Ck6VWVHWlx4Ur983o/edit), [일별 요청 예제](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/investor_trade_by_stock_daily/investor_trade_by_stock_daily.py), [일별 필드 매핑](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/domestic_stock/investor_trade_by_stock_daily/chk_investor_trade_by_stock_daily.py)
- 국내 지수 일봉 `FHKUP03500100` 예제는 국내 지수 경로와 OHLC 필드명을 확인해 준다. 지수·수급·거래대금 필드의 단위, 정밀도, FINAL 조건은 해당 공개 예제에서 확인되지 않았다. 해외 지수 예제는 미국 지수 조회 지원 대상을 Dow 30·Nasdaq 100·S&P 500으로 설명하고 `mod_yn`을 “변경 여부”로 매핑한다. [국내 지수 호출·필드 예제](https://github.com/koreainvestment/open-trading-api/blob/main/backtester/kis_backtest/providers/kis/data.py), [해외 지수 필드 매핑·지원 범위](https://github.com/koreainvestment/open-trading-api/blob/main/examples_llm/overseas_stock/inquire_daily_chartprice/chk_inquire_daily_chartprice.py)
- 공개 API 목록은 endpoint 이름·분류를 제공한다. 검토한 목록과 KIS 소유 예제에서는 FINAL 상태 필드, 확정 시각, `mod_yn` 값과 수정 이력의 관계, 정정 보존 기간을 찾지 못했다. [KIS Developers API 목록](https://apiportal.koreainvestment.com/apiservice-summary)

## 관측과 해석

기존 비밀 제거 응답 기록에는 같은 종목·날짜·요청 조건의 성공 응답에서 종가가 `206500`에서 다음 조회 `208000`으로 바뀌었고 두 응답 모두 `mod_yn=N`인 사례가 있다. 이는 성공 응답·장 마감·`mod_yn=N`만으로 불변 또는 FINAL을 판정할 수 없다는 관측이다. 최종 확정 시점을 이 사례로 추정하지 않는다. 세부 캡처 범위는 [기존 계약 기록](2026-11-13-kis-source-contract.md)을 참조한다.

## 단위 근거 범위

| 데이터 | 확인된 근거 | 남은 공백 |
|---|---|---|
| 종목 일봉 `FHKST03010100` | 호출 상한 100행, `FID_ORG_ADJ_PRC` 값의 endpoint별 의미 | OHLC·거래량·거래대금의 저장 단위·정밀도와 수정주가 산식 |
| 투자자 일별 `FHPTJ04160001` | 문서 사본의 `종목별 투자자매매동향(일별)` 탭: 금액 백만원, 수량 주 | FINAL 규칙, 필드별 반올림·공급자 저장 정밀도 |
| 국내 지수·업종 | endpoint·필드 이름과 일부 조회 상한 | 저장 단위·정밀도·수정 기준·FINAL 규칙 |
| 해외 지수 | 지원 인덱스와 일부 decimal 자릿수 표기 | 자릿수의 단위·저장 변환 의미·FINAL 규칙 |

## 실행 판정

- 공급자 FINAL 근거가 확보될 때까지 모든 원천 행은 `PROVISIONAL`로 유지하고 FINAL 기반 사용자·파생 조회에서 제외한다.
- 투자자 일별 `FHPTJ04160001`의 금액 백만원·수량 주 단위는 문서 사본으로 확인했다. 현재 코드의 해당 endpoint 금액 `×1,000,000` 변환은 이 단위와 일치한다. 가격·거래대금·지수·업종의 단위 및 정밀도는 endpoint별 문서 근거나 KIS 서면 확인 전까지 미확정이다.
- 원본 응답과 공급자 문자열을 보존하는 검증은 계속할 수 있다. 근거가 없는 단위 변환·FINAL 승격은 인수 근거로 사용하지 않는다.
- 이 재확인은 비밀 없는 KIS 문서 사본과 공개 예제를 다뤘다. FINAL 근거와 나머지 endpoint의 단위·정밀도 확인 없이는 #320의 FINAL·단위 인수를 완료할 수 없다.
