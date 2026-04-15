package org.stockwellness.application.sector.step.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorDailyDetailSnapshot;
import org.stockwellness.application.port.out.stock.SectorDataPort;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SectorDailyDetailItemProcessor implements ItemProcessor<MarketIndex, SectorDailyDetail> {

    private final SectorDataPort sectorDataPort;

    @Value("#{jobParameters['targetDate']}")
    private String targetDateStr;

    @Override
    public SectorDailyDetail process(MarketIndex marketIndex) {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now();

        if (!isKisEligibleIndexCode(marketIndex.getIndexCode())) {
            log.info("KIS 호출 대상이 아닌 market_index는 건너뜀. marketIndexCode={}, marketIndexName={}",
                    marketIndex.getIndexCode(), marketIndex.getIndexName());
            return null;
        }

        try {
            SectorDailyDetailSnapshot snapshot = sectorDataPort.fetchTodaySectorDetail(marketIndex.getIndexCode(), targetDate);
            if (snapshot.netForeignBuyAmount() == 0L && snapshot.netInstBuyAmount() == 0L) {
                log.debug("섹터 수급값이 모두 0으로 수집되었습니다. marketIndexCode={}, marketIndexName={}, targetDate={}",
                        marketIndex.getIndexCode(), marketIndex.getIndexName(), targetDate);
            }
            return SectorDailyDetail.of(marketIndex.getIndexCode(), marketIndex.getIndexName(), snapshot);
        } catch (Exception e) {
            log.warn("섹터 상세 원천 수집 건너뜀. marketIndexCode={}, marketIndexName={}, reason={}",
                    marketIndex.getIndexCode(), marketIndex.getIndexName(), e.getMessage());
            return null;
        }
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
