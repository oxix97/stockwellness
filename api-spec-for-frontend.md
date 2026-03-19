# StockWellness API Specification (for Frontend)

본 문서는 StockWellness 프로젝트의 React + TypeScript 개발자를 위한 API 명세서입니다. 모든 API는 표준화된 응답 포맷을 따르며, 강력한 타입 시스템을 지원합니다.

---

## 1. 공통 응답 포맷 (Common Response)

모든 API 응답은 아래의 `ApiResponse<T>` 구조로 래핑되어 전달됩니다.

```typescript
interface ApiResponse<T> {
  success: boolean;    // 요청 처리 성공 여부
  status: number;     // HTTP 상태 코드 (200, 201, 400, 500 등)
  code: string;       // 비즈니스 상세 코드 (예: "SUCCESS", "MEMBER_NOT_FOUND")
  message: string;    // 사용자 또는 개발자용 메시지
  data: T;            // 실제 데이터 (에러 시 null)
  timestamp: string;  // ISO 8601 응답 생성 시간
  traceId?: string;   // 에러 발생 시 추적을 위한 ID
  errors: FieldError[]; // 필드 레벨 검증 에러 상세 (성공 시 빈 리스트)
}

interface FieldError {
  field: string;      // 에러가 발생한 필드명
  value: string;      // 입력된 값
  reason: string;     // 에러 사유
}
```

---

## 2. 인증 및 보안 (Authentication)

- **방식**: Bearer Token (JWT)
- **헤더**: `Authorization: Bearer {accessToken}`
- **토큰 갱신**: Access Token 만료 시 `/api/v1/auth/reissue`를 통해 새로운 토큰 쌍을 발급받습니다.

---

## 3. API 상세 (Domain Specific)

### 3.1. 인증 (Auth)
- **Base URL**: `/api/v1/auth`

| 메서드 | 엔드포인트 | 설명 | 요청 데이터 | 응답 데이터 |
| :--- | :--- | :--- | :--- | :--- |
| POST | `/login` | 소셜 로그인 | `LoginRequest` | `LoginResponse` |
| POST | `/reissue` | 토큰 재발급 | `ReissueRequest` | `ReissueResponse` |
| POST | `/logout` | 로그아웃 | (Header Only) | `void` |

```typescript
interface LoginRequest {
  email: string;
  nickname: string;
  loginType: 'KAKAO' | 'GOOGLE' | 'NAVER';
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  memberId: number;
  email: string;
  nickname: string;
  joinedDate: string; // 가입 날짜 (예: "2022-01-01")
}
```

### 3.2. 포트폴리오 (Portfolio)
- **Base URL**: `/api/v1/portfolios`

| 메서드 | 엔드포인트 | 설명 | 요청 데이터 | 응답 데이터 |
| :--- | :--- | :--- | :--- | :--- |
| POST | `/` | 포트폴리오 생성 | `PortfolioCreateRequest` | `number` (ID) |
| GET | `/` | 내 포트폴리오 목록 | - | `PortfolioResponse[]` |
| GET | `/{id}` | 포트폴리오 상세 | - | `PortfolioResponse` |
| PUT | `/{id}` | 포트폴리오 수정 | `PortfolioUpdateRequest` | `void` |
| DELETE | `/{id}` | 포트폴리오 삭제 | - | `void` |
| GET | `/{id}/health` | 건강 진단 | - | `DiagnosisResponse` |
| GET | `/{id}/advice/latest` | 최신 AI 조언 | - | `AdviceResponse` |

### 3.3. 분석 (Portfolio Analysis)
- **Base URL**: `/api/v1/portfolios/{id}/analysis`

| 메서드 | 엔드포인트 | 설명 | 응답 데이터 |
| :--- | :--- | :--- | :--- |
| GET | `/valuation` | 수익률/가치 분석 | `PortfolioValuationResponse` |
| GET | `/diversification` | 분산 비중 분석 | `PortfolioDiversificationResponse` |
| GET | `/rebalancing` | 리밸런싱 가이드 | `PortfolioRebalancingResponse` |
| GET | `/summary` | 분석 요약 | `PortfolioAnalysisSummaryResponse` |
| POST | `/backtest` | 백테스팅 실행 | `BacktestResponse` |
| GET | `/correlation` | 종목 간 상관관계 | `Record<string, Record<string, number>>` |

### 3.4. 종목 (Stock)
- **Base URL**: `/api/v1/stocks`

| 메서드 | 엔드포인트 | 설명 | 쿼리 파라미터 | 응답 데이터 |
| :--- | :--- | :--- | :--- | :--- |
| GET | `/search` | 종목 통합 검색 | `keyword`, `marketType`, `page`, `size` | `Slice<StockSearchResult>` |
| GET | `/search/history` | 최근 검색어 | - | `string[]` |
| GET | `/{ticker}` | 종목 상세 정보 | - | `StockDetailResult` |
| GET | `/{ticker}/prices/history` | 차트 데이터 | `period`, `frequency` | `ChartDataResponse` |

### 3.5. 섹터 (Sector Dashboard)
- **Base URL**: `/api/v1/sectors`

| 메서드 | 엔드포인트 | 설명 | 응답 데이터 |
| :--- | :--- | :--- | :--- |
| GET | `/ranking/fluctuation` | 등락률 상위 섹터 | `SectorRankingResult[]` |
| GET | `/ranking/supply` | 수급 상위 섹터 | `SectorSupplyResult[]` |
| GET | `/{code}/comparison` | 섹터 vs 시장 비교 | `SectorComparisonResult` |

### 3.6. 배치 관리 (Batch Admin)
- **Base URL**: `/api/v1/admin/batch`
- **대상**: 관리자 도구 개발자 또는 데이터 상태 확인이 필요한 경우

| 메서드 | 엔드포인트 | 설명 | 응답 데이터 |
| :--- | :--- | :--- | :--- |
| POST | `/sync-indices` | 업종/지수 마스터 동기화 | `BatchExecutionResponse` |
| GET | `/status/{jobName}` | 특정 배치 Job 실행 상태 조회 | `BatchJobStatusResponse[]` |

#### 주요 배치 Job 목록 (jobName)
- `stockMasterSyncJob`: 전 종목 리스트 및 기본 정보 동기화
- `stockPriceBatchJob`: 일별 종가(EOD) 데이터 수집 및 기술 지표 계산
- `sectorEodJob`: 섹터별 등락률 및 수급 통계 산출
- `portfolioStatsJob`: 전체 포트폴리오 건강도 및 성과 지표 재계산

---

## 4. 데이터 갱신 정책 (Data Freshness)

프론트엔드 개발 시 데이터의 실시간성 여부를 판단하기 위한 가이드라인입니다.

1. **주식 시세 (Stock Price)**:
   - 본 프로젝트는 **EOD(End of Day)** 원칙을 따릅니다.
   - 장 중 실시간 데이터가 아닌, 전일 종가 또는 마감 직후 배치를 통해 수집된 데이터를 보여줍니다.
   - 데이터 갱신 주기: 매일 시장 마감 후(보통 16:00 ~ 18:00 사이) 순차적으로 반영됩니다.

2. **포트폴리오 분석 (Portfolio Analysis)**:
   - 사용자가 포트폴리오를 수정할 때 즉시 반영되지만, 성과 지표는 최신 EOD 데이터를 기반으로 합니다.

3. **섹터 및 랭킹 (Sector & Ranking)**:
   - `sectorEodJob` 실행 완료 후 최신화됩니다. API 호출 시 데이터가 전일자라면 배치 실행 상태를 확인하십시오.

---

## 5. 유의사항

1. **에러 처리**: `success`가 `false`인 경우 `code`를 확인하여 대응하는 UI 에러 메시지를 노출하십시오.
2. **날짜 포맷**: 모든 `timestamp`는 `YYYY-MM-DDTHH:mm:ss` 형식을 따릅니다.
3. **숫자 데이터**: 금액 및 수익률 데이터는 정밀도를 위해 `string` 또는 `number`로 전달될 수 있으니 확인 후 사용하십시오 (Java의 `BigDecimal` 대응).
4. **Slice/Page**: 검색 API는 페이징 처리를 위해 `Slice` 객체를 반환합니다. `last` 필드를 확인하여 무한 스크롤 구현이 가능합니다.

---

*본 명세서는 2026년 3월 19일 기준으로 작성되었습니다.*
