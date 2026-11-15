package org.stockwellness.application.service.marketdata;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@ExtendWith(MockitoExtension.class)
class MarketDataRevisionServiceTest {
    private static final LocalDate REVISED_DATE = LocalDate.of(2026, 10, 2);

    @Mock private StockPriceReadPort stockPrices;
    @Mock private IndexDailyReadPort indexPrices;
    @Mock private SectorLeadingStockPort sectorLeaders;
    @Mock private MarketDerivedDataUseCase derivedData;

    @Test
    void stock_correction_recalculates_only_its_next_252_final_observations_and_related_sector() {
        when(stockPrices.finalDatesFrom(7, "KRX", REVISED_DATE, 252))
                .thenReturn(List.of(REVISED_DATE, REVISED_DATE.plusDays(1), REVISED_DATE.plusDays(2)));
        when(sectorLeaders.seriesIdsForStock(7)).thenReturn(List.of(80L));
        var service = new MarketDataRevisionService(stockPrices, indexPrices, sectorLeaders, derivedData);

        service.recalculate(MarketDataRevisionEvent.stock(7, REVISED_DATE, "KRX", 2));

        verify(derivedData).calculateStockIndicators(7, "KRX", REVISED_DATE);
        verify(derivedData).calculateStockIndicators(7, "KRX", REVISED_DATE.plusDays(1));
        verify(derivedData).calculateStockIndicators(7, "KRX", REVISED_DATE.plusDays(2));
        verify(derivedData).calculateSectorLeadingStocks(80, REVISED_DATE);
        verifyNoMoreInteractions(derivedData);
    }

    @Test
    void index_correction_recalculates_only_its_next_252_final_observations() {
        when(indexPrices.finalDatesFrom(18, REVISED_DATE, 252))
                .thenReturn(List.of(REVISED_DATE, REVISED_DATE.plusDays(1)));
        var service = new MarketDataRevisionService(stockPrices, indexPrices, sectorLeaders, derivedData);

        service.recalculate(MarketDataRevisionEvent.index(18, REVISED_DATE, 3));

        verify(derivedData).calculateIndexIndicators(18, REVISED_DATE);
        verify(derivedData).calculateIndexWeather(18, REVISED_DATE);
        verify(derivedData).calculateSectorLeadingStocks(18, REVISED_DATE);
        verify(derivedData).calculateIndexIndicators(18, REVISED_DATE.plusDays(1));
        verify(derivedData).calculateIndexWeather(18, REVISED_DATE.plusDays(1));
        verifyNoMoreInteractions(derivedData);
    }
}
