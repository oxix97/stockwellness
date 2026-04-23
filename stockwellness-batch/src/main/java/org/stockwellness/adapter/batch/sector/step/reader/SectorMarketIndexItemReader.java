package org.stockwellness.adapter.batch.sector.step.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorDailyDetailPort;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SectorMarketIndexItemReader implements ItemReader<MarketIndex> {

    private final MarketIndexPort marketIndexPort;
    private final SectorDailyDetailPort sectorDailyDetailPort;

    @Value("#{jobParameters['targetDate']}")
    private String targetDateStr;

    private Iterator<MarketIndex> marketIndexIterator;

    @Override
    public MarketIndex read() {
        if (marketIndexIterator == null) {
            LocalDate targetDate = DateUtil.parseFlexible(targetDateStr);
            if (targetDate == null) {
                targetDate = DateUtil.today();
            }
            List<MarketIndex> allIndexes = marketIndexPort.findAll();
            List<MarketIndex> indexes = allIndexes.stream()
                    .filter(index -> isKisEligibleIndexCode(index.getIndexCode()))
                    .sorted(Comparator.comparing(MarketIndex::getIndexCode))
                    .toList();

            if (indexes.isEmpty()) {
                throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
            }

            sectorDailyDetailPort.deleteByBaseDateAndSectorCodeNotIn(
                    targetDate,
                    indexes.stream().map(MarketIndex::getIndexCode).toList()
            );
            log.info("섹터 상세 수집 대상 market_index 로드 완료. total={}, eligible={}", allIndexes.size(), indexes.size());
            marketIndexIterator = indexes.iterator();
        }

        return marketIndexIterator.hasNext() ? marketIndexIterator.next() : null;
    }

    private boolean isKisEligibleIndexCode(String indexCode) {
        if (Objects.isNull(indexCode)) {
            return false;
        }

        String normalized = indexCode.trim();
        if (normalized.isBlank()) {
            return false;
        }

        try {
            int parsed = Integer.parseInt(normalized);
            return parsed > 1 && parsed < 2000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
