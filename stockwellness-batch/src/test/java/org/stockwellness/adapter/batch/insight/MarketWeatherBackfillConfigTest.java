package org.stockwellness.adapter.batch.insight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.MarketIndexKind;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketWeatherBackfillConfigTest {
    private static final LocalDate AS_OF = LocalDate.of(2026, 11, 16);

    @Mock private JobRepository jobRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private IndexSeriesRepository indexSeriesRepository;
    @Mock private IndexDailyReadPort indexDailyReadPort;
    @Mock private MarketDerivedDataUseCase marketDerivedDataUseCase;

    private MarketWeatherBackfillConfig config;

    @BeforeEach
    void setUp() {
        config = new MarketWeatherBackfillConfig(jobRepository, transactionManager, indexSeriesRepository,
                indexDailyReadPort, marketDerivedDataUseCase);
    }

    @Test
    void backfills_recent_final_dates_for_canonical_korean_market_and_sector_series() {
        IndexSeries market = series(1, "kis.idxcode.KOSPI", "KOSPI", "KRW", true, MarketIndexKind.MARKET);
        IndexSeries sector = series(2, "kis.idxcode.KOSDAQ", "KOSDAQ", "KRW", true, MarketIndexKind.SECTOR);
        IndexSeries usMarket = series(3, "kis.ovrs.idxcode.N", "NASDAQ", "USD", true, MarketIndexKind.MARKET);
        IndexSeries nonCanonical = series(4, "kis.idxcode.KOSPI", "KOSPI", "KRW", false, MarketIndexKind.MARKET);
        when(indexSeriesRepository.findAll()).thenReturn(List.of(market, sector, usMarket, nonCanonical));
        LocalDate older = AS_OF.minusDays(2);
        LocalDate newer = AS_OF.minusDays(1);
        when(indexDailyReadPort.recentFinal(1L, AS_OF, 252)).thenReturn(List.of(observation(1, newer), observation(1, older)));
        when(indexDailyReadPort.recentFinal(2L, AS_OF, 252)).thenReturn(List.of(observation(2, newer)));

        config.backfillWeather(AS_OF);

        verify(marketDerivedDataUseCase).calculateIndexWeather(1L, older);
        verify(marketDerivedDataUseCase).calculateIndexWeather(1L, newer);
        verify(marketDerivedDataUseCase).calculateIndexWeather(2L, newer);
        verify(indexDailyReadPort, never()).recentFinal(3L, AS_OF, 252);
        verify(indexDailyReadPort, never()).recentFinal(4L, AS_OF, 252);
    }

    @Test
    void fails_when_no_supported_canonical_korean_series_exist() {
        when(indexSeriesRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> config.backfillWeather(AS_OF))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("canonical Korean index series");
    }

    private static IndexSeries series(long id, String namespace, String marketCode, String currency,
            boolean canonical, MarketIndexKind kind) {
        IndexSeries series = mock(IndexSeries.class);
        MarketIndex index = mock(MarketIndex.class);
        lenient().when(series.getId()).thenReturn(id);
        lenient().when(series.getProvider()).thenReturn("KIS");
        lenient().when(series.getProviderNamespace()).thenReturn(namespace);
        lenient().when(series.getCalendarMarketCode()).thenReturn(marketCode);
        lenient().when(series.getCurrency()).thenReturn(currency);
        lenient().when(series.getReturnBasis()).thenReturn("PRICE");
        lenient().when(series.getDividendBasis()).thenReturn("NONE");
        lenient().when(series.isActive()).thenReturn(true);
        lenient().when(series.isCanonical()).thenReturn(canonical);
        lenient().when(series.getMarketIndex()).thenReturn(index);
        lenient().when(index.getKind()).thenReturn(kind);
        return series;
    }

    private static IndexPriceObservation observation(long seriesId, LocalDate date) {
        return new IndexPriceObservation(seriesId, date, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                1, 1, 0, 0, 0, 1, "FINAL");
    }
}
