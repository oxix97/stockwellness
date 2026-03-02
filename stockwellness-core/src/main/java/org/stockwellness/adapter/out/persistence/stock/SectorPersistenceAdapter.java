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
import java.util.Collections;
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
        List<BigDecimal> prices = sectorInsightRepository.findPastPrices(sectorCode, date, PageRequest.of(0, limit));
        Collections.reverse(prices);
        return prices;
    }

    @Override
    public Map<String, SectorInsight> findLatestBeforeByCodes(List<String> sectorCodes, LocalDate date) {
        // [N+1 방지] 여러 섹터의 '가장 최근 데이터'를 가져와 Map으로 구성
        // findRecentSectorsByCodes 는 baseDate DESC 로 정렬되어 있으므로, 
        // toMap 에서 중복 키 발생 시 첫 번째(가장 최근) 데이터를 선택함.
        return sectorInsightRepository.findRecentSectorsByCodes(sectorCodes, date).stream()
                .collect(Collectors.toMap(
                        SectorInsight::getSectorCode,
                        s -> s,
                        (s1, s2) -> s1 
                ));
    }

    @Override
    public Map<String, List<BigDecimal>> findPastPricesByCodes(List<String> sectorCodes, LocalDate date, int limit) {
        // [N+1 방지] 여러 섹터의 과거 시계를 한 번에 로드
        List<SectorInsight> allHistory = sectorInsightRepository.findRecentSectorsByCodes(sectorCodes, date);
        
        return allHistory.stream()
                .collect(Collectors.groupingBy(
                        SectorInsight::getSectorCode,
                        Collectors.mapping(SectorInsight::getSectorIndexCurrentPrice, Collectors.toList())
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<BigDecimal> prices = e.getValue().stream().limit(limit).collect(Collectors.toList());
                            Collections.reverse(prices);
                            return prices;
                        }
                ));
    }

    @Override
    public List<SectorInsight> findByCodesAndDate(List<String> codes, LocalDate date) {
        return sectorInsightRepository.findBySectorCodeInAndBaseDate(codes, date);
    }

    @Override
    public List<SectorInsight> findHistoryByCode(String code, LocalDate endDate, int limit) {
        List<SectorInsight> history = sectorInsightRepository.findBySectorCodeAndBaseDateLessThanEqualOrderByBaseDateDesc(code, endDate, PageRequest.of(0, limit));
        Collections.reverse(history);
        return history;
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
