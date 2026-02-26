package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.in.stock.result.SectorSupplyResult;

import java.time.LocalDate;
import java.util.List;

public interface SectorInsightUseCase {
    /**
     * 모든 수집 대상 업종에 대해 최신 인사이트(지수, 수급, 주도주 등)를 동기화합니다.
     */
    void syncAllSectorInsights();

    /**
     * 당일 기준 등락률 상위 섹터 목록을 조회합니다.
     */
    List<SectorRankingResult> getTopSectorsByFluctuation(LocalDate date, int limit);

    /**
     * 당일 기준 수급(외인/기관) 상위 섹터 목록을 조회합니다.
     */
    List<SectorSupplyResult> getTopSectorsBySupply(LocalDate date, int limit);

    /**
     * 특정 섹터의 상세 인사이트 및 진단 정보를 조회합니다.
     */
    SectorDetailResult getSectorDetail(String sectorCode, LocalDate date);
}
