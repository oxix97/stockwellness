package org.stockwellness.batch.job.stock.price;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.fixture.StockFixture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
class StockPriceJdbcBatchItemWriterTest {

    @Autowired
    private JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Test
    @DisplayName("JdbcBatchItemWriter를 사용하여 대량의 시세 데이터를 효율적으로 저장/업데이트한다")
    void bulk_write_test() throws Exception {
        // Given
        Stock samsung = StockFixture.createSamsung();
        stockRepository.save(samsung);

        LocalDate date = LocalDate.of(2026, 3, 2);
        StockPrice price = StockPrice.of(
                samsung, date,
                new BigDecimal("70000"), new BigDecimal("71000"), new BigDecimal("69000"), new BigDecimal("70500"),
                new BigDecimal("70500"), new BigDecimal("70000"), // previousClosePrice added
                1000000L, new BigDecimal("70500000000"),
                TechnicalIndicators.empty()
        );

        // When
        stockPriceJdbcWriter.write(new Chunk<>(List.of(price)));

        // Then
        StockPrice saved = stockPriceRepository.findAllByIdBaseDate(date).stream()
                .filter(p -> p.getStock().getId().equals(samsung.getId()))
                .findFirst()
                .orElseThrow();
        
        assertThat(saved.getClosePrice()).isEqualByComparingTo("70500");
        assertThat(saved.getPreviousClosePrice()).isEqualByComparingTo("70000");
    }
}
