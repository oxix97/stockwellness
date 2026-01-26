package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.StockHistoryId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockHistoryRepository extends JpaRepository<StockHistory, StockHistoryId>, StockHistoryCustomRepository {
    /**
     * 특정 종목의 특정 기간 시세 조회 (AI 학습 및 차트용 핵심 쿼리)
     * <p>BaseDate 기준으로 정렬하여 반환</p>
     */
    List<StockHistory> findByIsinCodeAndBaseDateBetweenOrderByBaseDateAsc(
            String isinCode,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 특정 종목의 가장 최근 데이터 조회
     * <p>데이터 최신화 여부 확인용 (Last Updated Check)</p>
     */
    Optional<StockHistory> findTopByIsinCodeOrderByBaseDateDesc(String isinCode);

    /**
     * 특정 날짜의 전 종목 시세 조회
     * <p>시장 전체 분석(Cross-sectional analysis)용</p>
     */
    List<StockHistory> findByBaseDate(LocalDate baseDate);

    List<StockHistory> findTop60ByIsinCodeAndBaseDateBeforeOrderByBaseDateAsc(String isinCode, LocalDate baseDate);

    List<StockHistory> findAllByIsinCodeOrderByBaseDateAsc(String isinCode);

    @Query("SELECT distinct isinCode FROM StockHistory")
    List<String> findAllIsinCodes();

}
