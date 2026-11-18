package org.stockwellness.application.service.insight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort.SectorWeatherScore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketWeatherServiceTest {

    @Mock
    private IndexDailyReadPort indexDailyReadPort;

    @Test
    void averages_latest_final_kospi_sector_scores_with_half_up_rounding() {
        LocalDate date = LocalDate.of(2026, 11, 13);
        when(indexDailyReadPort.latestWeatherDate("0001")).thenReturn(Optional.of(date));
        when(indexDailyReadPort.finalSectorWeatherScores("0001", date)).thenReturn(List.of(
                new SectorWeatherScore("001", "전기전자", new BigDecimal("90.00")),
                new SectorWeatherScore("002", "금융", new BigDecimal("30.00")),
                new SectorWeatherScore("003", "화학", new BigDecimal("55.00")),
                new SectorWeatherScore("004", "건설", new BigDecimal("71.00"))));

        var response = new MarketWeatherService(indexDailyReadPort).getLatestMarketWeather("KOSPI");

        assertThat(response).hasValueSatisfying(weather -> {
            assertThat(weather.baseDate()).isEqualTo(date);
            assertThat(weather.marketType()).isEqualTo("KOSPI");
            assertThat(weather.weatherScore()).isEqualTo(62);
            assertThat(weather.weatherState()).isEqualTo("PARTLY_CLOUDY");
            assertThat(weather.topSectors()).extracting("sectorCode").containsExactly("001", "004", "003");
            assertThat(weather.bottomSectors()).extracting("sectorCode").containsExactly("002", "003", "004");
        });
        verify(indexDailyReadPort).finalSectorWeatherScores("0001", date);
        verify(indexDailyReadPort).latestWeatherDate("0001");
        verify(indexDailyReadPort, never()).finalWeatherScore("0001", date);
    }

    @Test
    void uses_the_requested_market_and_its_latest_date_for_the_average() {
        LocalDate kosdaqDate = LocalDate.of(2026, 11, 12);
        when(indexDailyReadPort.latestWeatherDate("1001")).thenReturn(Optional.of(kosdaqDate));
        when(indexDailyReadPort.finalSectorWeatherScores("1001", kosdaqDate)).thenReturn(List.of(
                new SectorWeatherScore("101", "반도체", new BigDecimal("20.00")),
                new SectorWeatherScore("102", "바이오", new BigDecimal("40.00"))));

        var response = new MarketWeatherService(indexDailyReadPort).getLatestMarketWeather("KOSDAQ");

        assertThat(response).hasValueSatisfying(weather -> {
            assertThat(weather.baseDate()).isEqualTo(kosdaqDate);
            assertThat(weather.marketType()).isEqualTo("KOSDAQ");
            assertThat(weather.weatherScore()).isEqualTo(30);
        });
        verify(indexDailyReadPort).finalSectorWeatherScores("1001", kosdaqDate);
        verify(indexDailyReadPort).latestWeatherDate("1001");
        verify(indexDailyReadPort, never()).finalWeatherScore("1001", kosdaqDate);
    }

    @Test
    void reflects_recalculated_sector_scores_on_the_same_market_date() {
        LocalDate date = LocalDate.of(2026, 11, 13);
        when(indexDailyReadPort.latestWeatherDate("0001")).thenReturn(Optional.of(date));
        when(indexDailyReadPort.finalSectorWeatherScores("0001", date))
                .thenReturn(List.of(new SectorWeatherScore("001", "전기전자", new BigDecimal("40")),
                        new SectorWeatherScore("002", "금융", new BigDecimal("60"))))
                .thenReturn(List.of(new SectorWeatherScore("001", "전기전자", new BigDecimal("80")),
                        new SectorWeatherScore("002", "금융", new BigDecimal("60"))));
        var service = new MarketWeatherService(indexDailyReadPort);

        var beforeCorrection = service.getLatestMarketWeather("KOSPI");
        var afterCorrection = service.getLatestMarketWeather("KOSPI");

        assertThat(beforeCorrection).hasValueSatisfying(weather -> assertThat(weather.weatherScore()).isEqualTo(50));
        assertThat(afterCorrection).hasValueSatisfying(weather -> assertThat(weather.weatherScore()).isEqualTo(70));
    }

    @Test
    void has_no_market_weather_when_no_sector_scores_exist_on_the_latest_date() {
        LocalDate date = LocalDate.of(2026, 11, 13);
        when(indexDailyReadPort.latestWeatherDate("0001")).thenReturn(Optional.of(date));
        when(indexDailyReadPort.finalSectorWeatherScores("0001", date)).thenReturn(List.of());

        assertThat(new MarketWeatherService(indexDailyReadPort).getLatestMarketWeather("KOSPI")).isEmpty();
    }

    @Test
    void rejects_unsupported_market_without_reading_data() {
        assertThat(new MarketWeatherService(indexDailyReadPort).getLatestMarketWeather("SPX")).isEmpty();
        verifyNoInteractions(indexDailyReadPort);
    }
}
