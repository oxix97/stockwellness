package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.SectorDailyDetailRepository;
import org.stockwellness.application.port.out.stock.SectorDailyDetailPort;
import org.stockwellness.application.port.out.stock.SectorDailyDetailSnapshot;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Component
@RequiredArgsConstructor
public class SectorDailyDetailPersistenceAdapter implements SectorDailyDetailPort {

    private final SectorDailyDetailRepository sectorDailyDetailRepository;

    @Override
    @Transactional
    public void saveAll(List<? extends SectorDailyDetail> details) {
        if (details.isEmpty()) {
            return;
        }

        LocalDate baseDate = validateSingleBaseDate(details.stream().map(SectorDailyDetail::getBaseDate).toList());
        List<String> sectorCodes = details.stream()
                .map(SectorDailyDetail::getSectorCode)
                .distinct()
                .toList();

        Map<String, SectorDailyDetail> existingMap = sectorDailyDetailRepository
                .findBySectorCodeInAndBaseDate(sectorCodes, baseDate)
                .stream()
                .collect(toMap(SectorDailyDetail::getSectorCode, detail -> detail));

        List<SectorDailyDetail> toSave = new ArrayList<>();
        for (SectorDailyDetail detail : details) {
            SectorDailyDetail existing = existingMap.get(detail.getSectorCode());
            if (existing != null) {
                existing.update(detail.getSectorName(), toSnapshot(detail));
                toSave.add(existing);
                continue;
            }
            toSave.add(detail);
        }

        sectorDailyDetailRepository.saveAll(toSave);
        sectorDailyDetailRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectorDailyDetail> findByBaseDate(LocalDate baseDate) {
        return sectorDailyDetailRepository.findByBaseDateOrderBySectorCodeAsc(baseDate);
    }

    @Override
    @Transactional
    public void deleteByBaseDateAndSectorCodeNotIn(LocalDate baseDate, List<String> sectorCodes) {
        if (sectorCodes == null || sectorCodes.isEmpty()) {
            sectorDailyDetailRepository.deleteByBaseDate(baseDate);
            return;
        }
        sectorDailyDetailRepository.deleteByBaseDateAndSectorCodeNotIn(baseDate, sectorCodes);
    }

    private LocalDate validateSingleBaseDate(List<LocalDate> baseDates) {
        LocalDate baseDate = baseDates.getFirst();
        boolean hasDifferentBaseDate = baseDates.stream().anyMatch(date -> !baseDate.equals(date));
        if (hasDifferentBaseDate) {
            throw new IllegalArgumentException("SectorDailyDetail saveAll은 동일한 baseDate chunk만 지원합니다.");
        }
        return baseDate;
    }

    private SectorDailyDetailSnapshot toSnapshot(SectorDailyDetail detail) {
        return new SectorDailyDetailSnapshot(
                detail.getSectorCode(),
                detail.getBaseDate(),
                detail.getCurrentPrice(),
                detail.getChangeAmount(),
                detail.getChangeSign(),
                detail.getChangeRate(),
                detail.getAccumulatedVolume(),
                detail.getPreviousVolume(),
                detail.getAccumulatedTradingAmount(),
                detail.getPreviousTradingAmount(),
                detail.getOpenPrice(),
                detail.getHighPrice(),
                detail.getLowPrice(),
                detail.getRisingIssueCount(),
                detail.getUpperLimitIssueCount(),
                detail.getSteadyIssueCount(),
                detail.getFallingIssueCount(),
                detail.getLowerLimitIssueCount(),
                detail.getYearlyHighPrice(),
                detail.getYearlyHighRate(),
                detail.getYearlyHighDate(),
                detail.getYearlyLowPrice(),
                detail.getYearlyLowRate(),
                detail.getYearlyLowDate(),
                detail.getTotalAskResidualQuantity(),
                detail.getTotalBidResidualQuantity(),
                detail.getSellResidualRate(),
                detail.getBuyResidualRate(),
                detail.getNetBuyResidualQuantity(),
                detail.getNetForeignBuyAmount(),
                detail.getNetInstBuyAmount()
        );
    }
}
