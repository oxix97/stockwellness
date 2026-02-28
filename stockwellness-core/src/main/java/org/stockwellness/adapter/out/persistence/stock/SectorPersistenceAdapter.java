package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.SectorInsightRepository;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SectorPersistenceAdapter implements SectorInsightPort, MarketIndexPort {

    private final SectorInsightRepository sectorInsightRepository;
    private final MarketIndexRepository marketIndexRepository;

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
    public void save(SectorInsight sectorInsight) {
        sectorInsightRepository.findBySectorCodeAndBaseDate(
                sectorInsight.getSectorCode(), 
                sectorInsight.getBaseDate()
        ).ifPresentOrElse(
            existing -> updateEntity(existing, sectorInsight),
            () -> sectorInsightRepository.save(sectorInsight)
        );
    }

    @Override
    public void saveAll(List<SectorInsight> sectorInsights) {
        if (sectorInsights.isEmpty()) return;

        LocalDate targetDate = sectorInsights.get(0).getBaseDate();
        Map<String, SectorInsight> existingMap = sectorInsightRepository.findAllByBaseDate(targetDate).stream()
                .collect(Collectors.toMap(SectorInsight::getSectorCode, s -> s));

        for (SectorInsight insight : sectorInsights) {
            SectorInsight existing = existingMap.get(insight.getSectorCode());
            if (existing != null) {
                updateEntity(existing, insight);
            } else {
                sectorInsightRepository.save(insight);
            }
        }
    }

    private void updateEntity(SectorInsight existing, SectorInsight source) {
        existing.update(
            source.getSectorIndexCurrentPrice(),
            source.getAvgFluctuationRate(),
            source.getNetForeignBuyAmount(),
            source.getNetInstBuyAmount(),
            source.getForeignConsecutiveBuyDays(),
            source.getInstConsecutiveBuyDays(),
            source.getTechnicalIndicators(),
            source.isOverheated()
        );
        existing.updateLeadingStocks(source.getLeadingStocks());
    }

    @Override
    public Optional<SectorInsight> findLatestBefore(String sectorCode, LocalDate date) {
        return sectorInsightRepository.findFirstBySectorCodeAndBaseDateBeforeOrderByBaseDateDesc(sectorCode, date);
    }

    @Override
    public List<MarketIndex> findAll() {
        return marketIndexRepository.findAll();
    }
}
