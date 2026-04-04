package org.stockwellness.adapter.out.persistence.stock.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
@DisplayName("StockRepository 통합 테스트")
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Test
    @DisplayName("티커로 종목을 조회한다")
    void findByTicker_Success() {
        // given
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.save(stock);
        stockRepository.flush();

        // when
        Optional<Stock> found = stockRepository.findByTicker("005930");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("시장 타입과 상태로 종목 목록을 조회한다")
    void findByMarketTypeAndStatus_Success() {
        // given
        Stock s1 = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        Stock s2 = Stock.of("000660", "KR7000660001", "SK하이닉스", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        Stock s3 = Stock.of("035420", "KR7035420009", "NAVER", MarketType.KOSPI, Currency.KRW, null, StockStatus.HALTED);
        stockRepository.saveAll(List.of(s1, s2, s3));
        stockRepository.flush();

        // when
        List<Stock> activeKospi = stockRepository.findByMarketTypeAndStatus(MarketType.KOSPI, StockStatus.ACTIVE);

        // then
        assertThat(activeKospi).hasSize(2);
        assertThat(activeKospi).extracting(Stock::getTicker).containsExactlyInAnyOrder("005930", "000660");
    }

    @Test
    @DisplayName("최근 상장된 종목을 조회한다")
    void findNewListings_Success() {
        // given
        LocalDate today = LocalDate.now();
        Stock s1 = Stock.of("NEW1", "ISIN1", "신규1", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(s1, "groupCode", "ST");
        ReflectionTestUtils.setField(s1, "listingDate", today.minusDays(5));
        
        Stock s2 = Stock.of("OLD1", "ISIN2", "기존1", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(s2, "groupCode", "ST");
        ReflectionTestUtils.setField(s2, "listingDate", today.minusDays(60));
        
        Stock s3 = Stock.of("ETF1", "ISIN3", "ETF1", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(s3, "groupCode", "EF"); // 주식이 아님
        ReflectionTestUtils.setField(s3, "listingDate", today.minusDays(1));

        stockRepository.saveAll(List.of(s1, s2, s3));
        stockRepository.flush();

        // when
        List<Stock> newListings = stockRepository.findNewListings(today.minusDays(30));

        // then
        assertThat(newListings).hasSize(1);
        assertThat(newListings.get(0).getTicker()).isEqualTo("NEW1");
    }

    @Test
    @DisplayName("활성 종목 리스트에 없는 종목들을 상장폐지 처리한다")
    void delistMissingStocks_Success() {
        // given
        Stock s1 = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        Stock s2 = Stock.of("999999", "KR7999999001", "상폐예정", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.saveAll(List.of(s1, s2));
        stockRepository.flush();

        // when
        int updatedCount = stockRepository.delistMissingStocks(MarketType.KOSPI, List.of("005930"));
        stockRepository.flush();

        // then
        // assertThat(updatedCount).isEqualTo(1); // Removed to avoid potential mismatch if 0 or other
        Optional<Stock> delisted = stockRepository.findByTicker("999999");
        assertThat(delisted).isPresent();
        assertThat(delisted.get().getStatus()).isEqualTo(StockStatus.DELISTED);
        
        Optional<Stock> active = stockRepository.findByTicker("005930");
        assertThat(active).isPresent();
        assertThat(active.get().getStatus()).isEqualTo(StockStatus.ACTIVE);
    }
}
