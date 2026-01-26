package org.stockwellness.adapter.out.persistence.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.config.TestJpaConfig;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryRepository;
import org.stockwellness.domain.stock.StockHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, TestJpaConfig.class})
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Test
    @DisplayName("종목명 키워드로 검색이 가능하다")
    void searchByName() {
        // given
        stockRepository.save(Stock.create("KR7005930003", "삼성전자", "005930", MarketType.KOSPI, 5969782550L, "1101110003201", "삼성전자"));
        stockRepository.save(Stock.create("KR7000660001", "SK하이닉스", "000660", MarketType.KOSPI, 728002365L, "1101110013567", "SK하이닉스"));

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Slice<Stock> result = stockRepository.searchByCondition("삼성", null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("티커 키워드로 검색이 가능하다")
    void searchByTicker() {
        // given
        stockRepository.save(Stock.create("KR7005930003", "삼성전자", "005930", MarketType.KOSPI, 5969782550L, "1101110003201", "삼성전자"));

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Slice<Stock> result = stockRepository.searchByCondition("005930", null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTicker()).isEqualTo("005930");
    }

    @Test
    @DisplayName("시장 구분 필터가 작동한다")
    void filterByMarketType() {
        // given
        stockRepository.save(Stock.create("KR7005930003", "삼성전자", "005930", MarketType.KOSPI, 5969782550L, "1101110003201", "삼성전자"));
        stockRepository.save(Stock.create("KR7323410001", "카카오게임즈", "293490", MarketType.KOSDAQ, 82433054L, "1101110003201", "카카오게임즈"));

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Slice<Stock> result = stockRepository.searchByCondition(null, MarketType.KOSDAQ, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("카카오게임즈");
    }

    @Test
    @DisplayName("최근 시세 데이터를 지정한 개수만큼 조회할 수 있다")
    void findRecentHistory() {
        // given
        String isin = "KR7005930003";
        stockHistoryRepository.save(StockHistory.create(isin, LocalDate.of(2024, 1, 1), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, 100L, BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000)));
        stockHistoryRepository.save(StockHistory.create(isin, LocalDate.of(2024, 1, 2), BigDecimal.valueOf(110), BigDecimal.valueOf(100), BigDecimal.valueOf(110), BigDecimal.valueOf(100), BigDecimal.valueOf(10), BigDecimal.valueOf(10), 110L, BigDecimal.valueOf(11000), BigDecimal.valueOf(1100000)));
        stockHistoryRepository.save(StockHistory.create(isin, LocalDate.of(2024, 1, 3), BigDecimal.valueOf(120), BigDecimal.valueOf(110), BigDecimal.valueOf(120), BigDecimal.valueOf(110), BigDecimal.valueOf(10), BigDecimal.valueOf(9), 120L, BigDecimal.valueOf(12000), BigDecimal.valueOf(1200000)));

        // when
        List<StockHistory> result = stockHistoryRepository.findRecentHistory(isin, LocalDate.of(2024, 1, 3), 2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBaseDate()).isEqualTo(LocalDate.of(2024, 1, 3));
        assertThat(result.get(1).getBaseDate()).isEqualTo(LocalDate.of(2024, 1, 2));
    }
}
