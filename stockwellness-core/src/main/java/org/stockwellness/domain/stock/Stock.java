package org.stockwellness.domain.stock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;
import static org.stockwellness.domain.stock.StockStatus.*;

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
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Stock extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String ticker; // AAPL, 005930

    @Column(length = 20)
    private String standardCode; // ISIN Code

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private MarketType marketType;

    @Enumerated(STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(length = 10)
    private String sectorCode;

    @Column(length = 50)
    private String sectorName;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private StockStatus status;

    // 포트폴리오에 담긴 종목인지 여부 (정밀 수집 대상)
    @Column(nullable = false)
    private boolean isPremiumTracking;

    public static Stock of(
            String ticker,
            String standardCode,
            String name,
            MarketType marketType,
            Currency currency,
            String sectorCode,
            String sectorName,
            StockStatus status

    ) {
        Stock stock = new Stock();
        stock.ticker = ticker.replaceAll("[^0-9A-Z]", "");
        stock.standardCode = standardCode;
        stock.name = name;
        stock.marketType = marketType;
        stock.currency = currency;
        stock.sectorCode = sectorCode;
        stock.sectorName = sectorName;
        stock.status = status;
        stock.isPremiumTracking = false;
        return stock;
    }

    public static Stock ofMasterFile(
            String ticker,
            String standardCode,
            String name,
            MarketType marketType,
            String rawSector
    ) {
        // Enum 로직 적용 (SectorCode.of는 이전 답변 로직 유지)
        SectorCode sector = SectorCode.of(rawSector, name);

        return Stock.of(
                ticker.replaceAll("[^0-9A-Z]", ""), // Ticker 정제
                standardCode,
                name.trim(),
                marketType,
                Currency.KRW,
                sector.getCode(),
                sector.getLabel(),
                StockStatus.ACTIVE
        );
    }

    // --- Business Logic ---
    public void enablePremiumTracking() {
        this.isPremiumTracking = true;
    }

    // 거래 정지 처리
    public void haltTrading() {
        this.status = HALTED;
    }

    // 거래 재개
    public void resumeTrading() {
        this.status = ACTIVE;
    }

    // 관리 종목 지정
    public void designateAsAdministrative() {
        this.status = ADMINISTRATIVE;
    }

    // 상장 폐지 (수집 중단)
    public void delist() {
        this.status = DELISTED;
        this.isPremiumTracking = false;
    }
}