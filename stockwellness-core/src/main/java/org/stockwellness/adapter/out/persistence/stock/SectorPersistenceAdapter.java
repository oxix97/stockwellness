package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.SectorInsightRepository;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorPersistenceAdapter implements SectorInsightPort, MarketIndexPort {

    private final SectorInsightRepository sectorInsightRepository;
    private final MarketIndexRepository marketIndexRepository;

    @Override
    @Transactional
    public void save(SectorInsight sectorInsight) {
        saveAll(List.of(sectorInsight));
    }

    @Override
    @Transactional
    public void saveAll(List<SectorInsight> insights) {
        for (SectorInsight insight : insights) {
            Optional<SectorInsight> existingOpt = sectorInsightRepository
                    .findBySectorCodeAndBaseDate(insight.getSectorCode(), insight.getBaseDate());

            if (existingOpt.isPresent()) {
                SectorInsight existing = existingOpt.get();
                existing.update(
                        insight.getSectorIndexCurrentPrice(),
                        insight.getAvgFluctuationRate(),
                        insight.getNetForeignBuyAmount(),
                        insight.getNetInstBuyAmount(),
                        insight.getForeignConsecutiveBuyDays(),
                        insight.getInstConsecutiveBuyDays(),
                        insight.getTechnicalIndicators(),
                        insight.isOverheated() 
                );
                existing.updateLeadingStocks(insight.getLeadingStocks());
                sectorInsightRepository.saveAndFlush(existing);
            } else {
                sectorInsightRepository.saveAndFlush(insight);
            }
        }
    }

    @Override
    public Optional<SectorInsight> findLatestBefore(String sectorCode, LocalDate date) {
        return sectorInsightRepository.findFirstBySectorCodeAndBaseDateBeforeOrderByBaseDateDesc(sectorCode, date);
    }

    @Override
    public Map<String, SectorInsight> findLatestBeforeByCodes(List<String> sectorCodes, LocalDate date) {
        return sectorInsightRepository.findRecentSectorsByCodes(sectorCodes, date).stream()
                .collect(Collectors.toMap(SectorInsight::getSectorCode, s -> s, (s1, s2) -> s1));
    }

    @Override
    public Map<String, List<BigDecimal>> findPastPricesByCodes(List<String> sectorCodes, LocalDate date, int limit) {
        Map<String, List<BigDecimal>> result = new HashMap<>();
        for (String code : sectorCodes) {
            result.put(code, findPastPrices(code, date, limit));
        }
        return result;
    }

    @Override
    public List<SectorInsight> findTopSectorsByFluctuation(LocalDate date, MarketType marketType, int limit) {
        return sectorInsightRepository.findTopByDate(date, marketType, PageRequest.of(0, limit));
    }

    @Override
    public List<SectorInsight> findTopSectorsBySupply(LocalDate date, MarketType marketType, int limit) {
        return sectorInsightRepository.findTopBySupply(date, marketType, PageRequest.of(0, limit));
    }

    @Override
    public Optional<SectorInsight> findBySectorCodeAndDate(String sectorCode, LocalDate date) {
        return sectorInsightRepository.findBySectorCodeAndBaseDate(sectorCode, date);
    }

    @Override
    public List<BigDecimal> findPastPrices(String sectorCode, LocalDate date, int limit) {
        return sectorInsightRepository.findPastPrices(sectorCode, date, PageRequest.of(0, limit));
    }

    @Override
    public List<SectorInsight> findByCodesAndDate(List<String> codes, LocalDate date) {
        return sectorInsightRepository.findBySectorCodeInAndBaseDate(codes, date);
    }

    @Override
    public List<SectorInsight> findHistoryByCode(String code, LocalDate endDate, int limit) {
        return sectorInsightRepository.findBySectorCodeAndBaseDateLessThanEqualOrderByBaseDateDesc(code, endDate, PageRequest.of(0, limit));
    }

    @Override
    public List<MarketIndex> findAll() {
        return marketIndexRepository.findAll();
    }
}
