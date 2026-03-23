# API 목록 — stockwellness-api

Base URL: `/api/v1`

> 인증 컬럼: ✓ = JWT 필수, ✗ = 불필요, optional = 인증 시 추가 기능

---

## Auth — `/api/v1/auth`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| POST | `/api/v1/auth/login` | 소셜 로그인 | ✗ |
| POST | `/api/v1/auth/reissue` | 토큰 재발급 | ✗ |
| POST | `/api/v1/auth/logout` | 로그아웃 | ✓ |
| GET | `/api/v1/auth/test` | 헬스체크용 테스트 | ✗ |

---

## Member — `/api/v1/members`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| GET | `/api/v1/members/me` | 내 정보 조회 | ✓ |
| PUT | `/api/v1/members/me` | 내 정보 수정 | ✓ |
| DELETE | `/api/v1/members/me` | 회원 탈퇴 | ✓ |

---

## Portfolio — `/api/v1/portfolios`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| POST | `/api/v1/portfolios` | 포트폴리오 생성 | ✓ |
| GET | `/api/v1/portfolios` | 내 포트폴리오 목록 | ✓ |
| GET | `/api/v1/portfolios/{portfolioId}` | 포트폴리오 상세 | ✓ |
| PUT | `/api/v1/portfolios/{portfolioId}` | 포트폴리오 수정 | ✓ |
| DELETE | `/api/v1/portfolios/{portfolioId}` | 포트폴리오 삭제 | ✓ |
| GET | `/api/v1/portfolios/{portfolioId}/health` | 건강 진단 | ✓ |
| GET | `/api/v1/portfolios/{portfolioId}/advice/latest` | 최신 AI 리밸런싱 조언 | ✓ |

### Portfolio Analysis — `/api/v1/portfolios/{portfolioId}/analysis`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| GET | `/api/v1/portfolios/{portfolioId}/analysis/valuation` | 가치 및 수익률 분석 | ✓ |
| GET | `/api/v1/portfolios/{portfolioId}/analysis/diversification` | 비중 분석 (자산군/업종/국가) | ✓ |
| GET | `/api/v1/portfolios/{portfolioId}/analysis/rebalancing` | 리밸런싱 가이드 | ✓ |
| GET | `/api/v1/portfolios/{portfolioId}/analysis/summary` | 분석 요약 (3종 통합) | ✓ |
| POST | `/api/v1/portfolios/{portfolioId}/analysis/backtest` | 백테스팅 시뮬레이션 | ✓ |
| GET | `/api/v1/portfolios/{portfolioId}/analysis/correlation` | 종목 간 상관관계 행렬 | ✓ |

---

## Stock — `/api/v1/stocks`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| GET | `/api/v1/stocks/search` | 종목 통합 검색 | optional |
| GET | `/api/v1/stocks/search/history` | 최근 검색어 조회 | optional |
| DELETE | `/api/v1/stocks/search/history` | 최근 검색어 개별 삭제 | optional |
| DELETE | `/api/v1/stocks/search/history/all` | 최근 검색어 전체 삭제 | optional |
| GET | `/api/v1/stocks/popular-search` | 인기 검색어 Top 10 | ✗ |
| GET | `/api/v1/stocks/new-listings` | 신규 상장 종목 | ✗ |
| GET | `/api/v1/stocks/{ticker}` | 종목 상세 정보 | ✗ |
| GET | `/api/v1/stocks/{ticker}/prices/history` | 차트용 과거 가격 데이터 | ✗ |
| GET | `/api/v1/stocks/{ticker}/returns` | 기간별 수익률 | ✗ |

---

## Sector — `/api/v1/sectors`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| GET | `/api/v1/sectors/ranking/fluctuation` | 등락률 상위 섹터 | ✗ |
| GET | `/api/v1/sectors/ranking/supply` | 수급 상위 섹터 | ✗ |
| GET | `/api/v1/sectors/{sectorCode}/comparison` | 섹터 vs 시장 비교 | ✗ |
| GET | `/api/v1/sectors/{sectorCode}/detail` | 섹터 상세 및 진단 | ✗ |

---

## Watchlist — `/api/v1/watchlist`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| POST | `/api/v1/watchlist/groups` | 그룹 생성 | ✓ |
| GET | `/api/v1/watchlist/groups` | 그룹 목록 조회 | ✓ |
| PATCH | `/api/v1/watchlist/groups/{groupId}` | 그룹명 수정 | ✓ |
| DELETE | `/api/v1/watchlist/groups/{groupId}` | 그룹 삭제 | ✓ |
| POST | `/api/v1/watchlist/groups/{groupId}/items` | 종목 추가 | ✓ |
| DELETE | `/api/v1/watchlist/groups/{groupId}/items/{ticker}` | 종목 삭제 | ✓ |
| PATCH | `/api/v1/watchlist/groups/{groupId}/items/{ticker}/note` | 메모 수정 | ✓ |
| GET | `/api/v1/watchlist/groups/{groupId}/items` | 종목 목록 조회 | ✓ |

---

## Admin — `/api/v1/admin`

| Method | Path | 설명 | 인증 |
|--------|------|------|:----:|
| GET | `/api/v1/admin/health` | 인프라 헬스체크 (DB/Redis/Kafka) | Admin |

---

총 **38개** 엔드포인트
