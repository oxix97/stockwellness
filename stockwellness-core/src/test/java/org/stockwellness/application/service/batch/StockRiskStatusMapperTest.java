package org.stockwellness.application.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

class StockRiskStatusMapperTest {
    private final StockRiskStatusMapper mapper = new StockRiskStatusMapper();

    @Test
    void complete_kosdaq_snapshot_preserves_clearing_trade_without_marking_stock_delisted() {
        Stock stock = mock(Stock.class);
        given(stock.getId()).willReturn(73L);
        given(stock.getStatus()).willReturn(StockStatus.ACTIVE);
        KosdaqItem item = completeItem();
        given(item.clearingTrade()).willReturn("Y");

        var result = mapper.fromKosdaq(stock, item);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().isClearingTrade()).isTrue();
        assertThat(result.orElseThrow().getStatus()).isEqualTo(StockStatus.ACTIVE);
        assertThat(result.orElseThrow().getSourceUpdatedAt()).isNull();
    }

    @Test
    void incomplete_or_unsupported_source_values_are_withheld_instead_of_defaulted() {
        Stock stock = mock(Stock.class);
        given(stock.getId()).willReturn(73L);
        KosdaqItem item = completeItem();
        given(item.investCaution()).willReturn(null);

        assertThat(mapper.fromKosdaq(stock, item)).isEmpty();
        assertThat(mapper.fromKospi(stock, null)).isEmpty();
    }

    private static KosdaqItem completeItem() {
        KosdaqItem item = mock(KosdaqItem.class);
        given(item.tradingHalt()).willReturn("N");
        given(item.clearingTrade()).willReturn("N");
        given(item.administeredStock()).willReturn("N");
        given(item.marketWarningLevel()).willReturn("00");
        given(item.warningNotice()).willReturn("N");
        given(item.unfaithfulDisclosure()).willReturn("N");
        given(item.backdoorListing()).willReturn("N");
        given(item.shortTermOverheat()).willReturn("N");
        given(item.shortSellOverheat()).willReturn("N");
        given(item.abnormalSurge()).willReturn("N");
        given(item.investCaution()).willReturn("N");
        return item;
    }
}
