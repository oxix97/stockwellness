package org.stockwellness.application.service.batch;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockRiskStatus;
import org.stockwellness.domain.stock.StockStatus;

@Component
public class StockRiskStatusMapper {
    public Optional<StockRiskStatus> fromKospi(Stock stock, KospiItem item) {
        if (stock == null || item == null) return Optional.empty();
        // The current KOSPI master record does not expose the KOSDAQ-only invest-caution flag.
        return Optional.empty();
    }

    public Optional<StockRiskStatus> fromKosdaq(Stock stock, KosdaqItem item) {
        if (stock == null || item == null || !yn(item.tradingHalt()) || !yn(item.clearingTrade())
                || !yn(item.administeredStock()) || !warning(item.marketWarningLevel())
                || !yn(item.warningNotice()) || !yn(item.unfaithfulDisclosure()) || !yn(item.backdoorListing())
                || !yn(item.shortTermOverheat()) || !yn(item.shortSellOverheat()) || !yn(item.abnormalSurge())
                || !yn(item.investCaution())) return Optional.empty();
        StockStatus status = "Y".equals(item.tradingHalt()) ? StockStatus.HALTED
                : "Y".equals(item.administeredStock()) ? StockStatus.ADMINISTRATIVE : StockStatus.ACTIVE;
        return Optional.of(StockRiskStatus.completeSnapshot(stock.getId(), status,
                "Y".equals(item.tradingHalt()), "Y".equals(item.clearingTrade()),
                "Y".equals(item.administeredStock()), item.marketWarningLevel(), "Y".equals(item.warningNotice()),
                "Y".equals(item.unfaithfulDisclosure()), "Y".equals(item.backdoorListing()),
                "Y".equals(item.shortTermOverheat()), "Y".equals(item.shortSellOverheat()),
                "Y".equals(item.abnormalSurge()), "Y".equals(item.investCaution()), "KIS_KOSDAQ_MST", null));
    }

    private static boolean yn(String value) { return "Y".equals(value) || "N".equals(value); }
    private static boolean warning(String value) {
        return "00".equals(value) || "01".equals(value) || "02".equals(value) || "03".equals(value);
    }
}
