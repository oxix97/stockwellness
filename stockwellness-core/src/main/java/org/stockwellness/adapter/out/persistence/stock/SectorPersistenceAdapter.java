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
    public void saveAll(List<? extends SectorInsight> insights) {
        if (insights.isEmpty()) {
            return;
        }

        LocalDate baseDate = validateSingleBaseDate(insights.stream().map(SectorInsight::getBaseDate).toList());
        List<String> sectorCodes = insights.stream()
                .map(SectorInsight::getSectorCode)
                .distinct()
                .toList();

        Map<String, SectorInsight> existingMap = sectorInsightRepository.findBySectorCodeInAndBaseDate(sectorCodes, baseDate).stream()
                .collect(Collectors.toMap(SectorInsight::getSectorCode, insight -> insight));

        List<SectorInsight> toSave = new ArrayList<>();
        for (SectorInsight insight : insights) {
            SectorInsight existing = existingMap.get(insight.getSectorCode());
            if (existing != null) {
                existing.update(
                        insight.getIndicators(),
                        insight.getTechnicalIndicators(),
                        insight.isOverheated()
                );
                existing.updateLeadingStocks(insight.getLeadingStocks());
                toSave.add(existing);
                continue;
            }
            toSave.add(insight);
        }

        sectorInsightRepository.saveAll(toSave);
        sectorInsightRepository.flush();
    }

    @Override
    public Optional<SectorInsight> findLatestBefore(String sectorCode, LocalDate date) {
        return sectorInsightRepository.findFirstBySectorCodeAndBaseDateBeforeOrderByBaseDateDesc(sectorCode, date);
    }

    @Override
    public Map<String, SectorInsight> findLatestBeforeByCodes(List<String> sectorCodes, LocalDate date) {
        return sectorInsightRepository.findLatestBeforeByCodes(sectorCodes, date);
    }

    @Override
    public Map<String, List<BigDecimal>> findPastPricesByCodes(List<String> sectorCodes, LocalDate date, int limit) {
        return sectorInsightRepository.findPastPricesByCodes(sectorCodes, date, limit);
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
    public List<SectorInsight> findAllByDate(LocalDate date, MarketType marketType) {
        if (marketType == null) {
            return sectorInsightRepository.findAllByBaseDate(date);
        }
        // marketType이 있는 경우 기존 쿼리 메서드 활용 (limit 없이 충분히 큰 Pageable 전달)
        return sectorInsightRepository.findTopByDate(date, marketType, PageRequest.of(0, 100));
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
    public Optional<LocalDate> findLatestDate() {
        return sectorInsightRepository.findMaxBaseDate();
    }

    @Override
    public List<MarketIndex> findAll() {
        return marketIndexRepository.findAll();
    }

    private LocalDate validateSingleBaseDate(List<LocalDate> baseDates) {
        LocalDate baseDate = baseDates.getFirst();
        boolean hasDifferentBaseDate = baseDates.stream().anyMatch(date -> !baseDate.equals(date));
        if (hasDifferentBaseDate) {
            throw new IllegalArgumentException("SectorInsight saveAll은 동일한 baseDate chunk만 지원합니다.");
        }
        return baseDate;
    }
}
