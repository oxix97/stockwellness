# Architecture

## 핵심 원칙

**Pragmatic Hexagonal Architecture**: 도메인 로직은 외부 인프라(DB, 외부 API)와 완전히 격리되어 `core` 모듈에 응집. 어댑터(`api`, `batch`)는 입력 변환과 출력 포매팅만 수행.

## 모듈 경계

```
stockwellness-core    ← 도메인·포트·서비스 (비즈니스 핵심)
stockwellness-api     ← core 의존, Web/Kafka 어댑터
stockwellness-batch   ← core 의존, 배치 잡·스케줄러
```

`api`와 `batch`는 서로를 의존하지 않는다.

## 레이어 구조

```
adapter/in/          Web (Controller), Kafka Consumer
    ↓ (UseCase 호출)
application/port/in/ UseCase 인터페이스
    ↓
application/service/ 비즈니스 로직 구현
    ↓ (Port 호출)
application/port/out/ Port 인터페이스
    ↑
adapter/out/         Persistence, External (KIS, OpenAI, Slack)

domain/              순수 도메인 모델 · 이벤트 · 예외 (외부 의존 없음)
```

## 구현 규칙

- 새 기능은 반드시 **UseCase(port/in) → Service → Adapter** 순으로 작성
- Controller는 UseCase만 호출, Service를 직접 주입받지 않는다
- Domain 모델은 인프라 어노테이션(`@Entity` 등) 포함 가능하나, 외부 API DTO를 직접 참조 금지
- 복잡한 조회 흐름은 Facade 패턴으로 진입점 단일화 (예: `PortfolioFacade`)

## 데이터 전략

| 데이터 성격 | 전략 |
|---|---|
| 읽기 전용·정적 (EOD 시세, 섹터 정보) | Redis 적극 캐싱 |
| 트랜잭션 데이터 (포트폴리오, 관심 종목) | PostgreSQL 트랜잭션 우선 |
| 장기 실행 작업 (AI 분석, 백테스트) | Kafka + Transactional Outbox 비동기 처리 |

## API 설계 원칙

- 엄격한 RESTful (리소스 URI, 표준 HTTP Method·Status Code)
- REST Docs + OpenAPI로 FE가 별도 문의 없이 연동 가능한 수준 유지

## 예외 처리 원칙

- 도메인별 커스텀 예외 사용 → `GlobalExceptionHandler`가 표준 에러 응답으로 변환
- 모든 API 요청·비동기 이벤트는 SLF4J + MDC로 추적 가능해야 함

> 예외 클래스 구조 및 코드 예시 → `@docs/code-style.md`

---

## 도메인 모델

### 서브도메인 구성

```
member      회원 · 인증 · 알림 설정
portfolio   포트폴리오 · 구성 종목 · AI 리밸런싱 리포트 · 건강 진단
stock       종목 마스터 · 일별 시세 · 기술 지표 · 섹터 · 배당
watchlist   관심 종목 그룹 · 아이템
shared      AbstractEntity · Email VO · 배치 이벤트
```

### 핵심 엔티티 관계

```
Member ────────────────────────────────────────────────────────────
  │ 1:N                                                            │ 1:N
  ▼                                                                ▼
Portfolio ──────────────── PortfolioStats (1:1)            WatchlistGroup
  │ 1:N                                                            │ 1:N
  ├── PortfolioItem (symbol → Stock)                        WatchlistItem
  └── AdvisorReport                                                │ N:1
                                                                 Stock
Stock ──────────────────────────────────────────────────────────────
  │ 1:N                    │ 1:N                    │ 1:N
  ▼                        ▼                        ▼
StockPrice            MarketSignal           DividendHistory
  └── TechnicalIndicators (Embedded)

SectorInsight                    BenchmarkPrice
  ├── SectorIndicators (Embedded)   (ticker, date → close)
  ├── TechnicalIndicators (Embedded)
  ├── SectorAiOpinion (Embedded)
  └── List<LeadingStock> (JSONB)

IndexConstituent (indexCode → stockTicker)
ExchangeRate (currencyPair, baseDate → rate)
```

### 서브도메인별 주요 타입

**Member**

| 타입 | 이름 | 설명 |
|---|---|---|
| Entity | `Member` | email(VO), nickname, loginType, riskLevel, status, 알림 설정 |
| Embeddable | `Email` | 정규식 검증 포함 불변 VO |
| Enum | `LoginType` | KAKAO / GOOGLE / NAVER / NONE |
| Enum | `MemberStatus` | PENDING → ACTIVE → DEACTIVATED |
| Enum | `RiskLevel` | LOW / MEDIUM / HIGH |
| Event | `MemberCreatedEvent` | 회원 가입 시 발행 |

**Portfolio**

| 타입 | 이름 | 설명 |
|---|---|---|
| Entity | `Portfolio` | memberId, name, items, advisorReports; 도메인 메서드로 items 교체 시 targetWeight 합계 검증(0% 또는 100%) |
| Entity | `PortfolioItem` | symbol, assetType(STOCK/CASH/BENCHMARK), quantity, purchasePrice, targetWeight |
| Entity | `PortfolioStats` | Portfolio 1:1, mdd, sharpeRatio, beta |
| Entity | `AdvisorReport` | content, action(REBALANCE/RISK_MANAGEMENT 등) |
| Enum | `AssetType` | STOCK / CASH / BENCHMARK |
| Enum | `BacktestStrategy` | LUMP_SUM(거치식) / DCA(적립식) |
| Policy | `BalanceScorePolicy` 외 5종 | 건강 진단 카테고리별 점수 정책 (전략 패턴) |

**Stock**

| 타입 | 이름 | 설명 |
|---|---|---|
| Entity | `Stock` | ticker(unique), 시장·통화·섹터·상태; 국내/해외 팩토리 메서드 분리 |
| Entity | `StockPrice` | EmbeddedId(stockId + baseDate), OHLCV, TechnicalIndicators |
| Entity | `SectorInsight` | 섹터 단위 기술 지표, AI 의견, 주도주 목록(JSONB) |
| Entity | `MarketSignal` | 거래량 급등, 골든 크로스 등 신호 감지 |
| Embeddable | `TechnicalIndicators` | MA5/20/60/120, RSI, MACD, Bollinger, ADX, 정배열 여부 |
| Embeddable | `StockSector` | 대·중·소 분류 코드 + 섹터명 |
| Record | `KospiItem` / `KosdaqItem` | KIS 마스터 파일 원본 레코드 (배치 전용) |
| Enum | `MarketType` | KOSPI / KOSDAQ / NASDAQ / NYSE / AMEX / INDEX |

**Watchlist**

| 타입 | 이름 | 설명 |
|---|---|---|
| Entity | `WatchlistGroup` | member 소유, 최대 10그룹 / 그룹당 50종목 제한, 소프트 삭제 |
| Entity | `WatchlistItem` | group + stock, ticker 비정규화, note(max 200자), 소프트 삭제 |

### 도메인 규칙 (주요)

- `Portfolio.updateItems()` — 아이템 교체 시 목표 비중 합계가 반드시 0% 또는 100%여야 함
- `WatchlistGroup.addItem()` — 그룹당 50종목 초과 및 중복 ticker 검증 내장
- `Member.activate()` / `deactivate()` — 상태 전이 메서드만 허용, 직접 필드 세팅 금지
- 소프트 삭제(`deletedAt`) — `WatchlistGroup`, `WatchlistItem` 적용; 하드 삭제 금지

---

## QueryDSL 패턴

### 기본 구조

```java
// ✅ Custom Repository 구현 패턴
@RequiredArgsConstructor
public class StockCustomRepositoryImpl implements StockCustomRepository {

    private final JPAQueryFactory queryFactory;  // 생성자 주입

    // Q-클래스는 static import 사용
    // import static org.stockwellness.domain.stock.QStock.stock;
}
```

### Null-safe 동적 조건

```java
// ✅ null이면 조건 무시, BooleanExpression 반환
private BooleanExpression marketTypeEq(MarketType marketType) {
    return marketType != null ? stock.marketType.eq(marketType) : null;
}

private BooleanExpression betweenRsi(BigDecimal rsiLow, BigDecimal rsiHigh) {
    if (rsiLow != null && rsiHigh != null) return stockPrice.indicators.rsi14.between(rsiLow, rsiHigh);
    if (rsiLow  != null)                   return stockPrice.indicators.rsi14.goe(rsiLow);
    if (rsiHigh != null)                   return stockPrice.indicators.rsi14.loe(rsiHigh);
    return null;
}
```

### 검색 랭킹 정렬 (CaseBuilder)

```java
// ✅ 정확 일치 → 이름 일치 → 시작 일치 → 포함 순으로 랭킹
private NumberExpression<Integer> rank(String keyword, StringPath name, StringPath ticker) {
    return new CaseBuilder()
            .when(ticker.equalsIgnoreCase(keyword)).then(1)
            .when(name.equalsIgnoreCase(keyword)).then(2)
            .when(name.startsWithIgnoreCase(keyword)).then(3)
            .otherwise(4);
}

queryFactory.selectFrom(stock)
        .where(keywordContains(keyword, name, ticker), marketTypeEq(marketType))
        .orderBy(rank(keyword, name, ticker).asc(), name.asc())
        ...
```

### Slice 페이지네이션

```java
// ✅ Page 대신 Slice — count 쿼리 없어 성능 우위
.offset(pageable.getOffset())
.limit(pageable.getPageSize() + 1)   // 1개 더 조회해서 hasNext 판단
.fetch();

boolean hasNext = content.size() > pageable.getPageSize();
if (hasNext) content.remove(pageable.getPageSize());

return new SliceImpl<>(content, pageable, hasNext);
```

### Fetch Join (N+1 방지)

```java
// ✅ 연관 엔티티를 한 번에 로드
queryFactory
        .selectFrom(stockPrice)
        .join(stockPrice.stock, stock).fetchJoin()   // N+1 방지
        .where(stock.in(stocks), stockPrice.id.baseDate.eq(baseDate))
        .fetch();
```

### Projections (DTO 직접 조회)

```java
// ✅ 필요한 필드만 선택 — 엔티티 전체 로드 대비 성능 개선
queryFactory
        .select(Projections.constructor(StockPriceResult.class,
                stockPrice.id.baseDate,
                stockPrice.openPrice,
                stockPrice.closePrice,
                stockPrice.indicators.ma5,
                stockPrice.indicators.ma20
        ))
        .from(stockPrice)
        .join(stockPrice.stock, stock)
        .where(stock.ticker.eq(ticker), stockPrice.id.baseDate.between(start, end))
        .orderBy(stockPrice.id.baseDate.asc())
        .fetch();
```

### 집계 + Map 변환

```java
// ✅ groupBy → Tuple → Map 변환
List<Tuple> results = queryFactory
        .select(stockPrice.stock.id, stockPrice.id.baseDate.max())
        .from(stockPrice)
        .where(stockPrice.stock.in(stocks))
        .groupBy(stockPrice.stock.id)
        .fetch();

return results.stream().collect(Collectors.toMap(
        t -> t.get(stockPrice.stock.id),
        t -> t.get(stockPrice.id.baseDate.max())
));
```

---

## 캐싱 전략

### @Cacheable (Spring Cache + Redis)

```java
// ✅ 티커 + 연도 복합 키로 연간 시세 캐싱
@Cacheable(value = "stock_prices_v2", key = "#ticker + ':' + #year", unless = "#result == null")
public List<StockPriceResult> loadByTickerAndYear(String ticker, int year) { ... }
```

- `unless = "#result == null"` — 결과 없는 경우 캐시 미저장 (공캐시 방지)
- 캐시 키 네이밍: `<도메인>_<버전>` 형식 (`stock_prices_v2`)

### Redis Sorted Set (검색 히스토리 / 인기 검색어)

```java
// ✅ 검색 히스토리 — score=timestamp, 최대 10개 유지
private static final String KEY_PREFIX = "search:history:";

redisTemplate.opsForZSet().add(key, keyword, (double) System.currentTimeMillis());
// 초과 시 오래된 것 제거
if (size > MAX_HISTORY_SIZE) {
    redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_HISTORY_SIZE - 1);
}
// 최신순 조회
redisTemplate.opsForZSet().reverseRange(key, 0, MAX_HISTORY_SIZE - 1);

// ✅ 인기 검색어 — 일자별 키, 2일 TTL 자동 만료
private static final String KEY_PREFIX = "search:rank:";  // 예: search:rank:20260327
private static final Duration KEY_TTL  = Duration.ofDays(2);

redisTemplate.opsForZSet().incrementScore(key, keyword, 1.0);
redisTemplate.expire(key, KEY_TTL);
redisTemplate.opsForZSet().reverseRange(key, 0, 9);  // Top 10
```

### Redis 키 네이밍 규칙

| 패턴 | 예시 | 용도 |
|---|---|---|
| `<domain>:<type>:<id>` | `search:history:42` | 회원별 검색 히스토리 |
| `<domain>:<type>:<date>` | `search:rank:20260327` | 날짜별 인기 검색어 |
| `<domain>_v<N>` (Spring Cache) | `stock_prices_v2` | 어노테이션 기반 캐시 |
