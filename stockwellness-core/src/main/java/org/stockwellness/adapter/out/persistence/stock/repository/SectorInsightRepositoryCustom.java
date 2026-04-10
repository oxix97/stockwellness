package org.stockwellness.adapter.out.persistence.stock.repository;

import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SectorInsightRepositoryCustom {

    Map<String, SectorInsight> findLatestBeforeByCodes(List<String> sectorCodes, LocalDate date);

    Map<String, List<BigDecimal>> findPastPricesByCodes(List<String> sectorCodes, LocalDate date, int limit);
}
