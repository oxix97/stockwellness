# Design Spec: 금융 데이터 수집 파이프라인 및 대용량 배치 처리

- **작성일:** 2026-05-07
- **상태:** Draft (Approved by User)
- **관련 모듈:** `stockwellness-batch`, `stockwellness-core`

## 1. 개요 (Overview)
본 문서는 한국투자증권(KIS) API를 연동하여 약 4,000개 종목의 일일 시세를 수집하고, 이를 바탕으로 최근 120일치 데이터를 참조하여 기술적 지표(RSI, MACD, MA 등)를 계산하는 대용량 배치 파이프라인의 설계를 정의한다.

## 2. 비즈니스 요구사항 및 제약사항
- **대상 규모:** 약 4,000개 상장 종목
- **데이터 범위:** EOD(End of Day) 시세 및 최근 120일치 가격 히스토리
- **성능 목표:** 
    - 외부 API 호출 병목 최소화 (Virtual Threads 활용)
    - 메모리 효율성 (Chunk 기반 처리)
    - DB I/O 최적화 (Bulk Upsert)
- **실행 주기:** 매 영업일 장 마감 후 (15:45 KST 예정)

## 3. 시스템 아키텍처 (Hexagonal Architecture)

### 3.1 인바운드 어댑터 (Inbound Adapter)
- **StockPriceBatchJob:** Spring Batch Job 선언 및 스케줄링.
- **DailyBatchOrchestrator:** Job 실행 순서 및 파라미터 제어.

### 3.2 아웃바운드 포트 및 어댑터 (Outbound Port & Adapter)
- **KisDailyPricePort / Adapter:** KIS API 연동 및 데이터 정규화.
- **StockPricePort / PersistenceAdapter:** `stock_price` 테이블 CRUD 및 히스토리 조회.

## 4. 파이프라인 상세 설계 (Step-by-Step)

### Step 1: `collectStockPriceStep` (가격 수집)
1. **Reader:** DB에서 활성 상태(`ACTIVE`)인 모든 종목 리스트를 읽어옴.
2. **Processor:** 
    - `VirtualThreadPerTaskExecutor`를 사용하여 KIS API를 병렬로 호출.
    - API 응답 데이터를 `StockPrice` 도메인 모델로 변환.
3. **Writer:** `JdbcBatchItemWriter`를 사용하여 `stock_price` 테이블에 `Upsert`.
    - SQL: `INSERT INTO stock_price (...) ON CONFLICT (ticker, base_date) DO UPDATE SET ...`

### Step 2: `technicalIndicatorCalculateStep` (지표 계산)
1. **Reader:** 활성 종목 리스트를 청크 단위로 읽어옴.
2. **Processor:** 
    - 각 종목당 최근 120일치 가격 데이터를 DB에서 조회.
    - `TechnicalIndicatorCalculator`를 통해 RSI, MACD, 이동평균선 등을 계산.
    - 계산된 지표를 `StockPrice` 엔티티의 `indicators` 필드에 반영.
3. **Writer:** 계산 결과가 포함된 `StockPrice` 정보를 DB에 반영.

## 5. 기술적 고려사항

### 5.1 성능 최적화
- **I/O Parallelism:** 4,000번의 외부 API 호출 시 Virtual Threads를 적용하여 처리량(Throughput) 극대화.
- **Caching:** 지표 계산 시 반복되는 종목 조회를 최소화하기 위해 필요 시 청크 단위 대량 조회(In-query) 검토.

### 5.2 안정성 및 가용성
- **Rate Limiting:** KIS API의 호출 제한(TPS)을 준수하기 위한 클라이언트 측 Throttling 적용.
- **Idempotency:** 배치 재실행 시 데이터가 중복되거나 오염되지 않도록 모든 쓰기 작업은 `Upsert` 기준.
- **Logging & Alert:** `LoggingAspect`를 통한 구조화된 로그 기록 및 작업 실패 시 Slack 알림 연동.

## 6. 데이터 모델 (Schema)
- **stock_price:** `base_date`, `ticker`를 복합키로 사용. 시가, 고가, 저가, 종가 및 각종 기술 지표 컬럼 포함.

---

## Spec Self-Review
- [x] **Placeholder scan:** TBD/TODO 없음. 모든 수치와 기술 명칭 확정.
- [x] **Internal consistency:** Hexagonal Architecture와 Spring Batch 패턴 일치 확인.
- [x] **Scope check:** 단일 Job 내의 2개 Step으로 명확히 구분되어 구현 가능함.
- [x] **Ambiguity check:** Upsert 방식 및 계산 참조 범위(120일) 명시함.
