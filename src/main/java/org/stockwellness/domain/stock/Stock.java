package org.stockwellness.domain.stock;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static org.stockwellness.domain.stock.StockStatus.ACTIVE;
import static org.stockwellness.domain.stock.StockStatus.DELISTED;

/**
 * 주식 종목 마스터 엔티티
 * <p>
 * 금융위원회 KRX 상장종목정보를 원천으로 합니다.
 * 변하지 않는 기준 정보(Master Data)를 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    /**
     * ISIN 코드 (국제 채권 식별 번호)
     * <p>ex: KR7000020008</p>
     * 불변의 Primary Key로 사용됩니다.
     */
    @Id
    @Column(length = 12, nullable = false)
    private String isinCode;

    /**
     * 단축 코드 (티커)
     * <p>ex: A000020</p>
     */
    @Column(length = 9, nullable = false)
    private String ticker;

    /**
     * 종목명
     * <p>ex: 동화약품</p>
     */
    @Column(nullable = false)
    private String name;

    /**
     * 시장 구분
     * <p>KOSPI, KOSDAQ, KONEX</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private MarketType marketType;

    /**
     * 상장 주식 수
     * <p>Source: lstgStCnt</p>
     * <p>매일 변하지 않지만, 증자/감자 등으로 변경될 수 있음.
     * 마스터 데이터로 관리하여 최신 상태를 유지함.</p>
     */
    private Long totalShares;

    /**
     * 상장 상태
     * <p>ACTIVE(거래중), DELISTED(상장폐지)</p>
     */
    @Enumerated(EnumType.STRING)
    private StockStatus status;

    @Column(length = 20)
    private String corporationNo;

    @Column(length = 50)
    private String corporationName;

    public static Stock create(String isinCode, String name, String ticker, MarketType marketType, Long totalShares, String corporationNo, String corporationName) {
        Stock stock = new Stock();
        stock.isinCode = isinCode;
        stock.updateInfo(name, ticker, marketType, totalShares, corporationNo, corporationName);
        return stock;
    }

    /**
     * API 정보를 통해 마스터 데이터 갱신
     * (상장주식수 변경 등을 반영)
     */
    public void updateInfo(String name, String ticker, MarketType marketType, Long totalShares, String corporationNo, String corporationName) {
        this.name = name;
        this.ticker = ticker;
        this.marketType = marketType;
        this.totalShares = totalShares;
        this.status = StockStatus.ACTIVE; // 데이터가 들어오면 활성 상태로 간주
        this.corporationNo = corporationNo;
        this.corporationName = corporationName;
    }

    public void delisted() {
        this.status = DELISTED;
    }

    public void active() {
        this.status = ACTIVE;
    }
}