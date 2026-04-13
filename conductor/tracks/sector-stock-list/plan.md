# 구현 계획: 업종별 종목 리스트 조회 기능

오늘의 업종 지수 랭킹 아이템 클릭 시, 해당 업종에 속한 모든 종목을 조회할 수 있도록 백엔드 API를 확장하고 프론트엔드 검색 기능을 연동합니다.

## 1. 개요
- **목표**: 업종 랭킹 아이템 클릭 -> 바텀시트에서 "전체 종목 보기" 클릭 -> 해당 업종 종목 리스트 노출
- **관련 기능**: [기능 8] 섹터별 종목 리스트, [기능 26] 오늘의 섹터 랭킹

## 2. 상세 구현 단계

### Phase 1: 백엔드 검색 API 확장 (stockwellness-core/api)
- [x] `SearchStockQuery` 레코드에 `String sectorCode` 추가
- [x] `StockController.searchStocks`에서 `sectorCode` 쿼리 파라미터 수신 및 `SearchStockQuery`에 매핑
- [x] `StockUseCase` 및 `StockService`에서 `sectorCode` 필터링 로직 추가
- [x] `StockPersistenceAdapter` (QueryDSL)에서 `sectorCode`가 존재할 경우 `stock.sector.code.eq(sectorCode)` 조건 추가
- [x] 통합 테스트 코드를 통해 업종 필터링 검색 결과 검증

### Phase 2: 프론트엔드 API 및 훅 업데이트 (stockwellness-front)
- [x] `src/api/stock.ts`: `search` 함수에 `sectorCode` 파라미터 추가
- [x] `src/hooks/use-search.ts`: `useSearch` 훅이 `sectorCode` 상태를 관리하고 `useInfiniteQuery`에 포함하도록 수정
- [x] `src/types/api.ts`: 검색 요청 관련 타입 업데이트 (필요 시)

### Phase 3: 프론트엔드 UI 연동 (stockwellness-front)
- [x] `src/app/components/screens/Search.tsx`: 
    - URL에서 `sectorCode` 및 `sectorName` 쿼리 파라미터 추출
    - 업종 필터가 활성화된 경우 UI에 표시 (예: "반도체 업종 검색 결과")
- [x] `src/app/components/home/SectorBottomSheet.tsx`:
    - 하단에 "이 업종의 모든 종목 보기" 버튼/링크 추가
    - 클릭 시 `/search?sectorCode=...&sectorName=...`으로 이동

## 3. 검증 계획
- [x] **API 검증**: `GET /api/v1/stocks/search?sectorCode=G001` 호출 시 해당 업종 종목만 반환되는지 확인
- [x] **UI 검증**: 홈 화면 -> 업종 클릭 -> 바텀시트 "전체 보기" -> 검색 화면 자동 필터링 확인
- [x] **무한 스크롤**: 업종 필터링된 결과에서도 무한 스크롤이 정상 작동하는지 확인
