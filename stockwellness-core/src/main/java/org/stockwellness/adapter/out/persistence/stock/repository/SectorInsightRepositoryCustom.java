package org.stockwellness.adapter.out.persistence.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.stockwellness.domain.stock.insight.SectorInsight;

public interface SectorInsightRepositoryCustom {

    Map<String, SectorInsight> findLatestBeforeByCodes(List<String> sectorCodes, LocalDate date);

    Map<String, List<BigDecimal>> findPastPricesByCodes(List<String> sectorCodes, LocalDate date, int limit);
}
