package org.stockwellness.adapter.out.persistence.stock.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.util.List;
import java.util.Optional;

/**
 * Stock 엔티티(종목 마스터) 접근을 위한 Repository
 * 기본적으로 Spring Data JPA를 사용합니다.
 */
public interface StockRepository extends JpaRepository<Stock, String> {
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
    List<Stock> findByIsinCodeIn(List<String> isinCodes);

    @Query("SELECT s FROM Stock s WHERE " +
            "(:keyword IS NULL OR s.name LIKE %:keyword% OR s.ticker LIKE %:keyword%) AND " +
            "(:marketType IS NULL OR s.marketType = :marketType) AND " +
            "(:status IS NULL OR s.status = :status)")
    Slice<Stock> searchByCondition(
            @Param("keyword") String keyword,
            @Param("marketType") MarketType marketType,
            @Param("status") StockStatus status,
            Pageable pageable
    );

    @Query("SELECT s.isinCode FROM Stock s")
    List<String> findAllByIsinCode();
}