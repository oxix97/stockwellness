package org.stockwellness.adapter.out.persistence.stock.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Stock 엔티티(종목 마스터) 접근을 위한 Repository
 * 기본적으로 Spring Data JPA를 사용합니다.
 */
public interface StockRepository extends JpaRepository<Stock, String>, StockCustomRepository {
    /**
     * 티커(단축코드)로 종목 조회
     * <p>ISIN 코드가 PK이지만, 사용자나 외부 API는 티커를 주로 사용함</p>
     */
    Optional<Stock> findByTicker(String ticker);

    /**
     * 특정 시장(KOSPI, KOSDAQ)에 속한 활성 종목 조회
     * <p>배치 작업 시 시장별로 나누어 처리할 때 유용</p>
     */
    List<Stock> findByMarketTypeAndStatus(MarketType marketType, StockStatus status);

    /**
     * 상장폐지되지 않은 모든 활성 종목 조회
     */
    List<Stock> findByStatus(StockStatus status);

    /**
     * ISIN 코드 목록으로 여러 종목 한 번에 조회 (IN절)
     */
    List<Stock> findByTickerIn(List<String> isinCodes);

    /**
     * 특정 업종 코드(mediumCode)에 속하는 활성 종목 조회
     */
    List<Stock> findBySector_MediumCodeAndStatus(String mediumCode, StockStatus status);

    /** 신규 상장 종목 조회 (최근 등록 순 10개)
     */
    List<Stock> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT s.ticker FROM Stock s")
    List<String> findAllByTicker();

    /** 배치 upsert용 — ticker 목록으로 벌크 조회 후 Map 변환 */
    default Map<String, Stock> findAsMapByTickers(Collection<String> tickers) {
        return findAllByTickerIn(tickers).stream()
                .collect(Collectors.toMap(Stock::getTicker, s -> s));
    }

    /** 단축코드 목록으로 벌크 조회 → Map<shortCode, entity> 변환에 활용 */
    List<Stock> findAllByTickerIn(Collection<String> tickers);

    /** 마스터 파일에서 사라진 국내 종목 상장폐지 처리 */
    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.status = 'DELISTED', s.isPremiumTracking = false
        WHERE s.marketType = :marketType
          AND s.status != 'DELISTED'
          AND s.ticker NOT IN :activeTickers
        """)
    int delistMissingStocks(@Param("marketType") MarketType marketType,
                            @Param("activeTickers") Collection<String> activeTickers);
}
