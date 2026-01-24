package org.stockwellness.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.fixture.StockFixture;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Stock 도메인 단위 테스트")
class StockTest {

    @Test
    @DisplayName("성공: 종목 마스터 정보를 생성하고 갱신한다")
    void create_and_update_stock_success() {
        // given
        Stock stock = StockFixture.createStock("KR7000000001", "000001", "테스트종목");

        // when
        stock.updateInfo("갱신종목", "000002", MarketType.KOSDAQ, 2000000L, "999", "갱신회사");

        // then
        assertThat(stock.getName()).isEqualTo("갱신종목");
        assertThat(stock.getTicker()).isEqualTo("000002");
        assertThat(stock.getMarketType()).isEqualTo(MarketType.KOSDAQ);
        assertThat(stock.getTotalShares()).isEqualTo(2000000L);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.ACTIVE);
    }

    @Test
    @DisplayName("성공: 일별 시세 엔티티에서 Candle VO로 변환한다")
    void to_candle_success() {
        // given
        LocalDate date = LocalDate.now();
        StockHistory history = StockHistory.create(
                "KR7005930003", date,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(49000),
                BigDecimal.valueOf(51000), BigDecimal.valueOf(48500),
                BigDecimal.valueOf(1000), BigDecimal.valueOf(2.0),
                100000L, BigDecimal.valueOf(5000000000L), BigDecimal.valueOf(300000000000000L)
        );

        // when
        StockCandle candle = history.toCandle();

        // then
        assertThat(candle.baseDate()).isEqualTo(date);
        assertThat(candle.close()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(candle.open()).isEqualTo(BigDecimal.valueOf(49000));
        assertThat(candle.high()).isEqualTo(BigDecimal.valueOf(51000));
        assertThat(candle.low()).isEqualTo(BigDecimal.valueOf(48500));
        assertThat(candle.volume()).isEqualTo(100000L);
    }

    @Test
    @DisplayName("성공: 기술적 지표들을 일괄 업데이트한다")
    void update_indicators_success() {
        // given
        StockHistory history = StockFixture.createHistory("KR7005930003", LocalDate.now(), 50000);
        TechnicalIndicators indicators = new TechnicalIndicators(
                BigDecimal.valueOf(49500), BigDecimal.valueOf(48000),
                BigDecimal.valueOf(47000), BigDecimal.valueOf(46000),
                BigDecimal.valueOf(65.5), BigDecimal.valueOf(500)
        );

        // when
        history.updateIndicators(indicators);

        // then
        assertThat(history.getMa5()).isEqualTo(BigDecimal.valueOf(49500));
        assertThat(history.getMa20()).isEqualTo(BigDecimal.valueOf(48000));
        assertThat(history.getRsi14()).isEqualTo(BigDecimal.valueOf(65.5));
        assertThat(history.getMacd()).isEqualTo(BigDecimal.valueOf(500));
    }
}
