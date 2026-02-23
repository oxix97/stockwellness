package org.stockwellness.domain.stock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

/**
 * 종목 통합 엔티티
 *
 * <p>국내(KOSPI/KOSDAQ) 종목은 KIS 마스터 파일 동기화 Job을 통해 자동 등록/갱신됩니다.
 * 해외(NASDAQ/NYSE/AMEX) 종목은 별도 수집 경로를 통해 등록됩니다.
 *
 * <p>KIS 마스터 데이터(국내 전용)는 임베디드 타입으로 분리되어 있습니다.
 * <ul>
 *   <li>{@link StockTradingStatus}  — 거래 상태 (거래정지 / 정리매매 / 관리종목)
 *   <li>{@link StockWarningStatus}  — 시장 경보 (시장경고 단계 / 불성실공시 / 우회상장 등)
 *   <li>{@link StockOverheatStatus} — 과열/급등 감시 (단기과열 / 공매도 과열 / 이상급등 등)
 * </ul>
 * 해외 종목의 경우 위 임베디드 필드는 모두 기본값(false / null)으로 유지됩니다.
 */
@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(
        name = "stock",
        indexes = {
                @Index(name = "idx_stock_ticker", columnList = "ticker", unique = true),
                @Index(name = "idx_stock_name", columnList = "name"),
                @Index(name = "idx_stock_status", columnList = "status"),
                @Index(name = "idx_stock_ticker_name", columnList = "ticker, name")
        }
)
public class Stock extends AbstractEntity {

    // ── 공통 식별 정보 ─────────────────────────────────────────────────────────

    /**
     * 단축코드 / 티커 (e.g. "005930", "AAPL")
     */
    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    /**
     * 표준코드 / ISIN (e.g. "KR7005930003")
     */
    @Column(length = 20)
    private String standardCode;

    /**
     * 종목명
     */
    @Column(nullable = false, length = 100)
    private String name;

    // ── 공통 분류 정보 ─────────────────────────────────────────────────────────

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private MarketType marketType;

    @Enumerated(STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "sector_large_name", length = 100)
    private String sectorLargeName;

    @Column(name = "sector_medium_name", length = 100)
    private String sectorMediumName;
    /**
     * 업종명 (e.g. "제약", "반도체").
     * {@link MarketIndex#getIndexName()}에서 가져옵니다.
     * 해외 종목 또는 매핑 실패 시 {@code null}.
     */
    @Column(name = "sector_small_name", length = 100)
    private String sectorSmallName;

    // ── 공통 상태 ──────────────────────────────────────────────────────────────

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private StockStatus status;

    /**
     * 포트폴리오 편입 종목 여부 (정밀 수집 대상)
     */
    @Column(nullable = false)
    private boolean isPremiumTracking;

    // ── 국내 전용: 종목 기본 정보 (KIS 마스터) ────────────────────────────────

    /**
     * 그룹코드 (ST=주식, EF=ETF 등). 해외 종목은 null.
     */
    @Column(name = "group_code", length = 5)
    private String groupCode;

    /**
     * 시가총액 규모 (1=대형, 2=중형, 3=소형, 0=기타). 해외 종목은 null.
     */
    @Column(name = "market_cap_size", length = 5)
    private String marketCapSize;

    /**
     * 상장일자. 해외 종목은 null.
     */
    @Column(name = "listing_date")
    private LocalDate listingDate;

    /**
     * 액면가 (원). 해외 종목은 null.
     */
    @Column(name = "par_value")
    private Long parValue;

    /**
     * 결산월. 해외 종목은 null.
     */
    @Column(name = "fiscal_month", length = 2)
    private String fiscalMonth;

    /**
     * 우선주 여부. 해외 종목은 false.
     */
    @Column(name = "is_preferred", nullable = false)
    private boolean isPreferred;

    // ── 국내 전용: 투자위험/주의 (KIS 마스터, 임베디드) ──────────────────────

    /**
     * 거래 상태 (거래정지 / 정리매매 / 관리종목). 해외 종목은 기본값.
     */
    @Embedded
    private StockTradingStatus tradingStatus;

    /**
     * 시장 경보 (시장경고 단계 / 경고예고 / 불성실공시 / 우회상장). 해외 종목은 기본값.
     */
    @Embedded
    private StockWarningStatus warningStatus;

    /**
     * 과열/급등 감시 (단기과열 / 공매도과열 / 이상급등 / 투자주의환기). 해외 종목은 기본값.
     */
    @Embedded
    private StockOverheatStatus overheatStatus;

    // ── 팩토리 메서드 ──────────────────────────────────────────────────────────

    /**
     * 해외 종목 생성 (KIS 마스터 데이터 없음)
     */
    public static Stock of(
            String ticker,
            String standardCode,
            String name,
            MarketType marketType,
            Currency currency,
            String sectorLargeName,
            String sectorMediumName,
            String sectorSmallName,
            StockStatus status
    ) {
        Stock s = new Stock();
        s.ticker = ticker.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
        s.standardCode = standardCode;
        s.name = name;
        s.marketType = marketType;
        s.currency = currency;
        s.sectorLargeName = sectorLargeName;
        s.sectorMediumName = sectorMediumName;
        s.sectorSmallName = sectorSmallName;
        s.status = status;
        s.isPremiumTracking = false;
        s.isPreferred = false;
        s.tradingStatus = StockTradingStatus.defaultValue();
        s.warningStatus = StockWarningStatus.defaultValue();
        s.overheatStatus = StockOverheatStatus.defaultValue();
        return s;
    }

    /**
     * 코스피 종목 생성 (KIS 마스터 데이터 포함)
     */
    public static Stock ofKospi(KospiItem item, String sectorLargeName,
                                String sectorMediumName,
                                String sectorSmallName) {
        Stock s = new Stock();
        s.ticker = item.shortCode();
        s.standardCode = item.isinCode();
        s.name = item.koreanName();
        s.marketType = MarketType.KOSPI;
        s.currency = Currency.KRW;
        s.sectorLargeName = sectorLargeName;
        s.sectorMediumName = sectorMediumName;
        s.sectorSmallName = sectorSmallName;
        s.status = resolveStatus(item);
        s.isPremiumTracking = false;
        s.groupCode = item.groupCode();
        s.marketCapSize = item.marketCapSize();
        s.listingDate = parseDate(item.listingDate());
        s.parValue = parseLong(item.parValue());
        s.fiscalMonth = item.fiscalMonth();
        s.isPreferred = !"0".equals(item.preferredStockType());
        s.tradingStatus = StockTradingStatus.ofKospi(item);
        s.warningStatus = StockWarningStatus.ofKospi(item);
        s.overheatStatus = StockOverheatStatus.ofKospi(item);
        return s;
    }

    /**
     * 코스닥 종목 생성 (KIS 마스터 데이터 포함)
     */
    public static Stock ofKosdaq(KosdaqItem item, String sectorLargeName,
                                 String sectorMediumName,
                                 String sectorSmallName) {
        Stock s = new Stock();
        s.ticker = item.shortCode();
        s.standardCode = item.isinCode();
        s.name = item.koreanName();
        s.marketType = MarketType.KOSDAQ;
        s.currency = Currency.KRW;
        s.sectorLargeName = sectorLargeName;
        s.sectorMediumName = sectorMediumName;
        s.sectorSmallName = sectorSmallName;
        s.status = resolveStatus(item);
        s.isPremiumTracking = false;
        s.groupCode = item.groupCode();
        s.marketCapSize = item.marketCapSize();
        s.listingDate = parseDate(item.listingDate());
        s.parValue = parseLong(item.parValue());
        s.fiscalMonth = item.fiscalMonth();
        s.isPreferred = !"0".equals(item.preferredStockType());
        s.tradingStatus = StockTradingStatus.ofKosdaq(item);
        s.warningStatus = StockWarningStatus.ofKosdaq(item);
        s.overheatStatus = StockOverheatStatus.ofKosdaq(item);
        return s;
    }

    // ── 변경 메서드 ────────────────────────────────────────────────────────────

    /**
     * KIS 마스터 최신 데이터로 갱신 (ticker, marketType, listingDate 제외)
     */
    public void updateFromKospi(KospiItem item, String sectorLargeName,
                                String sectorMediumName,
                                String sectorSmallName) {
        this.standardCode = item.isinCode();
        this.name = item.koreanName();
        this.sectorLargeName = sectorLargeName;
        this.sectorMediumName = sectorMediumName;
        this.sectorSmallName = sectorSmallName;
        this.status = resolveStatus(item);
        this.groupCode = item.groupCode();
        this.marketCapSize = item.marketCapSize();
        this.parValue = parseLong(item.parValue());
        this.fiscalMonth = item.fiscalMonth();
        this.isPreferred = !"0".equals(item.preferredStockType());
        this.tradingStatus = StockTradingStatus.ofKospi(item);
        this.warningStatus = StockWarningStatus.ofKospi(item);
        this.overheatStatus = StockOverheatStatus.ofKospi(item);
    }

    public void updateFromKosdaq(KosdaqItem item, String sectorLargeName,
                                 String sectorMediumName,
                                 String sectorSmallName) {
        this.standardCode = item.isinCode();
        this.name = item.koreanName();
        this.sectorLargeName = sectorLargeName;
        this.sectorMediumName = sectorMediumName;
        this.sectorSmallName = sectorSmallName;
        this.status = resolveStatus(item);
        this.groupCode = item.groupCode();
        this.marketCapSize = item.marketCapSize();
        this.parValue = parseLong(item.parValue());
        this.fiscalMonth = item.fiscalMonth();
        this.isPreferred = !"0".equals(item.preferredStockType());
        this.tradingStatus = StockTradingStatus.ofKosdaq(item);
        this.warningStatus = StockWarningStatus.ofKosdaq(item);
        this.overheatStatus = StockOverheatStatus.ofKosdaq(item);
    }

    // ── 비즈니스 메서드 ────────────────────────────────────────────────────────

    public void enablePremiumTracking() {
        this.isPremiumTracking = true;
    }

    public void haltTrading() {
        this.status = StockStatus.HALTED;
    }

    public void resumeTrading() {
        this.status = StockStatus.ACTIVE;
    }

    public void designateAsAdministrative() {
        this.status = StockStatus.ADMINISTRATIVE;
    }

    public void delist() {
        this.status = StockStatus.DELISTED;
        this.isPremiumTracking = false;
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    /**
     * KIS 마스터 필드 조합으로 {@link StockStatus}를 결정합니다.
     *
     * <pre>
     * 정리매매 → DELISTED      (상장폐지 확정, 정리매매 단계)
     * 거래정지  → HALTED
     * 관리종목  → ADMINISTRATIVE
     * 그 외    → ACTIVE
     * </pre>
     */
    private static StockStatus resolveStatus(KospiItem item) {
        if ("Y".equals(item.clearingTrade())) return StockStatus.DELISTED;
        if ("Y".equals(item.tradingHalt())) return StockStatus.HALTED;
        if ("Y".equals(item.administeredStock())) return StockStatus.ADMINISTRATIVE;
        return StockStatus.ACTIVE;
    }

    private static StockStatus resolveStatus(KosdaqItem item) {
        if ("Y".equals(item.clearingTrade())) return StockStatus.DELISTED;
        if ("Y".equals(item.tradingHalt())) return StockStatus.HALTED;
        if ("Y".equals(item.administeredStock())) return StockStatus.ADMINISTRATIVE;
        return StockStatus.ACTIVE;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw, DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseLong(String raw) {
        try {
            return Long.parseLong(raw.strip().replaceAll("^0+$", "0"));
        } catch (Exception e) {
            return null;
        }
    }
}