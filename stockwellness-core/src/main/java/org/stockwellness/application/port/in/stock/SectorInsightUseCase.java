package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.SectorComparisonResult;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.in.stock.result.SectorSupplyResult;
import org.stockwellness.domain.stock.MarketType;

import java.time.LocalDate;
import java.util.List;

public interface SectorInsightUseCase {
    /**
     * 모든 수집 대상 업종에 대해 최신 인사이트(지수, 수급, 주도주 등)를 동기화합니다.
     */
    void syncAllSectorInsights();

    /**
     * 특정 시장 및 날짜 기준 등락률 상위 섹터 목록을 조회합니다.
     */
    List<SectorRankingResult> getTopSectorsByFluctuation(LocalDate date, MarketType marketType, int limit);

    /**
     * 특정 시장 및 날짜 기준 수급(외인/기관) 상위 섹터 목록을 조회합니다.
     */
    List<SectorSupplyResult> getTopSectorsBySupply(LocalDate date, MarketType marketType, int limit);

    /**
     * 특정 섹터의 상세 인사이트 및 진단 정보를 조회합니다.
     */
    SectorDetailResult getSectorDetail(String sectorCode, LocalDate date);

    /**
     * 특정 섹터와 전체 시장(KOSPI/KOSDAQ) 지수를 비교 분석합니다.
     */
    SectorComparisonResult compareWithMarket(String sectorCode, LocalDate date);
}
