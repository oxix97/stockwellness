# API 연동 명세서 (홈 & 검색 탭)

이 문서는 홈 화면과 검색 탭에서 사용되는 주요 기능들의 프론트엔드 훅, API 호출 함수 및 백엔드 엔드포인트 연동 현황을 정리한 것입니다.

---

## 1. 홈 화면 (Home)

| 기능명 | 커스텀 훅 | API 함수 | 백엔드 엔드포인트 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **시장 현황** | `useMarketIndex` | `marketApi.getMarketIndexes()` | `GET /api/v1/market/indexes` | KOSPI, KOSDAQ, S&P500 등 지수 정보 |
| **내 포트폴리오 수익률** | `usePortfolioSummary` | `portfolioApi.getAnalysisSummary()` | `GET /api/v1/portfolios/{id}/analysis/summary` | 자산 가치, 수익률, 평가 손익 요약 |
| **AI 주목 섹터** | `useSector` | `sectorApi.getFluctuationRanking()` | `GET /api/v1/sectors/ranking/fluctuation` | 등락률 상위 3개 섹터 및 AI 분석 메시지 |
| **수급 상위 섹터** | `useSupply` | `sectorApi.getSupplyRanking()` | `GET /api/v1/sectors/ranking/supply` | 기관/외국인 순매수 및 연속 매수 상위 |
| **신규 상장** | `useStock().newListings` | `stockApi.getNewListings()` | `GET /api/v1/stocks/new-listings` | 최근 시장에 신규 상장된 종목 리스트 |

---

## 2. 검색 탭 (Search)

| 기능명 | 커스텀 훅 | API 함수 / 저장소 | 백엔드 엔드포인트 / 키 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **인기 검색어** | `useStock().popular` | `stockApi.getPopularSearch()` | `GET /api/v1/stocks/popular-search` | 실시간 인기 종목 검색어 리스트 |
| **종목 검색** | `useStock().useSearch` | `stockApi.search()` | `GET /api/v1/stocks/search` | 2글자 이상 입력 시 동작 (무한 스크롤) |
| **최근 검색어** | (컴포넌트 내 관리) | `localStorage` | `recent-searches` | 클라이언트 로컬 스토리지에 최대 10개 저장 |
| **종목 상세 이동** | (Link) | - | `/stock/{ticker}` | 상세 진입 시 `useDetail` 훅 호출 |

---

## 3. 관심 탭 (Watchlist)

| 기능명 | 커스텀 훅 | API 함수 | 백엔드 엔드포인트 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **그룹 목록 조회** | `useWatchlist().groups` | `watchlistApi.getGroups()` | `GET /api/v1/watchlist/groups` | 사용자의 관심 그룹 리스트와 각 그룹의 종목 수 |
| **종목 목록 조회** | `useWatchlist().useGroupItems` | `watchlistApi.getItems()` | `GET /api/v1/watchlist/groups/{id}/items` | 특정 그룹 내 종목 리스트 (RSI, AI 진단 포함) |
| **그룹 생성** | `createGroup` | `watchlistApi.createGroup()` | `POST /api/v1/watchlist/groups` | 새 관심 그룹 추가 |
| **종목 추가** | `addItem` | `watchlistApi.addItem()` | `POST /api/v1/watchlist/groups/{id}/items` | 선택한 종목을 특정 그룹에 추가 |
| **종목 삭제** | `removeItem` | `watchlistApi.removeItem()` | `DELETE /api/v1/watchlist/groups/{id}/items/{ticker}` | 그룹에서 특정 종목 제거 (스와이프 삭제 지원) |
| **메모 업데이트** | `updateItemNote` | `watchlistApi.updateItemNote()` | `PATCH /api/v1/watchlist/groups/{id}/items/{ticker}/note` | 종목별 개인 메모 저장 및 수정 |

---

## 4. 포트폴리오 (Portfolio)

| 기능명 | 커스텀 훅 | API 함수 | 백엔드 엔드포인트 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **포트폴리오 요약** | `usePortfolioSummary` | `portfolioApi.getAnalysisSummary()` | `GET /api/v1/portfolios/{id}/analysis/summary` | 자산 가치, 수익률, CAGR, 변동성, 알파, 기여도 등 (`startDate`, `endDate` 파라미터 지원) |
| **건강 진단** | `usePortfolioHealth` | `portfolioApi.getHealth()` | `GET /v1/portfolios/{id}/health` | 자산 배분 점수 및 레이더 차트 데이터 |
| **보유 종목 조회** | `usePortfolioDetails` | `portfolioApi.getHoldings()` | `GET /v1/portfolios/{id}` | 현재 보유 중인 종목 리스트 및 수량 |
| **AI 리밸런싱** | `usePortfolioAnalysis` | `portfolioApi.getRebalancing()` | `GET /v1/portfolios/{id}/analysis/rebalancing` | 현재 비중 vs 목표 비중 조정 제언 |
| **포트폴리오 생성/수정** | `useCreatePortfolio`, `useUpdatePortfolio` | `portfolioApi.create()`, `portfolioApi.updatePortfolio()` | `POST /v1/portfolios`, `PUT /v1/portfolios/{id}` | 포트폴리오 이름 및 종목 구성 변경 |

---

## 5. 백테스팅 (Backtesting)

| 기능명 | 커스텀 훅 | API 함수 | 백엔드 엔드포인트 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **시뮬레이션 실행** | `useBacktest` | `portfolioApi.runBacktest()` | `POST /v1/portfolios/{id}/analysis/backtest` | 과거 성과 시뮬레이션 (거치식/적립식) |
| **결과 요약 및 지표** | `useBacktest` | - | - | CAGR, MDD, Sharpe, Beta, Best/Worst Year |
| **AI 성과 코멘트** | `useBacktest` | - | - | 시뮬레이션 결과에 대한 AI 분석 메시지 |

---

## 6. 주식 상세 (Stock Detail)

| 기능명 | 커스텀 훅 | API 함수 | 백엔드 엔드포인트 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **주가 이력 조회** | `useStock().useHistory` | `stockApi.getPriceHistory()` | `GET /v1/stocks/{ticker}/prices/history` | 캔들 차트 데이터 (일/주/월봉) |
| **기간별 수익률** | `useStock().useReturns` | `stockApi.getReturns()` | `GET /v1/stocks/{ticker}/returns` | 1주/1달/3달/1년 수익률 및 벤치마크 비교 |
| **종목 상세 정보** | `useStock().useDetail` | `stockApi.getStockDetail()` | `GET /v1/stocks/{ticker}` | 종목명, 현재가 등 기본 정보 |
| **관심 종목 토글** | `useWatchlist` | `addItem`, `removeItem` | `POST /v1/watchlist/groups/...`, `DELETE ...` | 하트 아이콘 클릭 시 관심 등록/해제 |

---

## 7. 마이페이지 (More)

| 기능명 | 커스텀 훅 | API 함수 | 백엔드 엔드포인트 | 비고 |
| :--- | :--- | :--- | :--- | :--- |
| **닉네임 변경** | `useUpdateProfile` | `memberApi.updateNickname()` | `PATCH /v1/members/nickname` | 사용자 프로필(닉네임) 수정 |
| **회원 탈퇴** | `useWithdraw` | `memberApi.withdraw()` | `DELETE /v1/members` | 계정 삭제 및 데이터 파기 |
| **로그아웃** | (auth store) | - | - | 로컬 스토리지 토큰 삭제 및 세션 종료 |

---

## 8. 상세 설명

### 홈 화면 (Home)
- **시장 지수 연동**: 인사말(맑음/비/흐림)은 KOSPI의 등락률 데이터를 기반으로 동적으로 결정됩니다.
- **포트폴리오 요약**: `portfolioId`가 있는 경우(로그인 및 생성 완료)에만 노출되며, `AnalysisSummaryResponse` 타입을 통해 종합적인 자산 현황을 보여줍니다.
- **섹터 상세 조회**: `useSector` 훅 내부에서 랭킹 조회 후 각 섹터의 `sectorCode`를 이용해 상세 정보(`getSectorDetail`)를 병렬로 추가 조회하여 진단 메시지를 결합합니다.

### 검색 탭 (Search)
- **실시간 검색 최적화**: 검색어 입력 시 `useSearch` 무한 쿼리가 실행되며, `page` 파라미터를 통해 페이징 처리가 이루어집니다.
- **최근 검색어 저장 로직**: 검색어가 입력된 후 약 0.8초간 변화가 없을 때(Debounce 효과) 자동으로 로컬 스토리지에 저장됩니다.
- **인기 검색어 Fallback**: 비로그인 상태에서 401 에러가 발생하더라도 빈 배열로 처리하여 화면이 깨지지 않도록 구현되어 있습니다.
