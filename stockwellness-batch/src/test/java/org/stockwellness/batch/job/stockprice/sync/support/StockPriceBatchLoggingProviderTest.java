package org.stockwellness.batch.job.stockprice.sync.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.stockwellness.batch.support.logging.BatchFailureSummary;
import org.stockwellness.batch.support.logging.BatchLoggingConstants;
import org.stockwellness.batch.support.logging.BatchProgressSnapshot;
import org.stockwellness.batch.support.logging.BatchStartSummary;
import org.stockwellness.batch.support.listener.BatchFailureItemListener;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.StockPriceId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@MockitoSettings(strictness = Strictness.LENIENT)
class StockPriceBatchLoggingProviderTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Long> typedQuery;

    private StockPriceBatchLoggingProvider provider;

    @BeforeEach
    void setUp() {
        openMocks(this);
        provider = new StockPriceBatchLoggingProvider(entityManagerFactory);
    }

    @Test
    @DisplayName("시작 요약은 reader와 같은 조건의 totalCount를 계산해 ExecutionContext에 저장한다")
    void initializeStoresStartSummary() {
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution(
                "stockPriceBatchJob",
                1L,
                1L,
                new JobParametersBuilder()
                        .addString("startDate", "20240101")
                        .addString("endDate", "20240131")
                        .addString("targetTicker", "005930")
                        .toJobParameters()
        );

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createQuery(anyString(), org.mockito.ArgumentMatchers.<Class<Long>>any())).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(7L);

        BatchStartSummary summary = provider.initialize(jobExecution);

        assertThat(summary.startDate()).isEqualTo("20240101");
        assertThat(summary.endDate()).isEqualTo("20240131");
        assertThat(summary.totalCount()).isEqualTo(7L);
        assertThat(jobExecution.getExecutionContext().getLong(BatchLoggingConstants.CTX_TOTAL_COUNT)).isEqualTo(7L);
    }

    @Test
    @DisplayName("진행률은 StockPrice row 수가 아니라 distinct stock id 수 기준으로 누적한다")
    void updateProgressCountsDistinctStocks() {
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution("stockPriceBatchJob", 1L, 1L);
        jobExecution.setStartTime(LocalDateTime.now().minusMinutes(5));
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_TOTAL_COUNT, 10L);
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_PROCESSED_COUNT, 0L);

        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobExecution, "stockPriceStep", 1L);
        Chunk<List<StockPrice>> chunk = new Chunk<>(List.of(List.of(
                stockPrice(1L, "005930", LocalDate.of(2024, 1, 1)),
                stockPrice(1L, "005930", LocalDate.of(2024, 1, 2)),
                stockPrice(2L, "000660", LocalDate.of(2024, 1, 1))
        )));

        provider.updateProgress(stepExecution, chunk);
        BatchProgressSnapshot snapshot = provider.getProgressSnapshot(jobExecution);

        assertThat(snapshot.processedCount()).isEqualTo(2L);
        assertThat(snapshot.totalCount()).isEqualTo(10L);
        assertThat(snapshot.currentItemId()).isEqualTo(2L);
        assertThat(snapshot.currentItemKey()).isEqualTo("000660");
        assertThat(snapshot.progressPercent()).isEqualTo(20.0d);
        assertThat(snapshot.estimatedRemainingMs()).isNotNull();
    }

    @Test
    @DisplayName("실패 요약은 마지막 실패 종목 ID와 ticker를 반환한다")
    void getFailureSummaryReturnsLastFailedItem() {
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution("stockPriceBatchJob", 1L, 1L);
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobExecution, "stockPriceStep", 1L);
        stepExecution.getExecutionContext().put(BatchFailureItemListener.LAST_FAILED_ITEM_ID, "2451");
        stepExecution.getExecutionContext().put(BatchFailureItemListener.LAST_FAILED_ITEM_KEY, "005930");

        BatchFailureSummary summary = provider.getFailureSummary(jobExecution);

        assertThat(summary.failedItemId()).isEqualTo(2451L);
        assertThat(summary.failedItemKey()).isEqualTo("005930");
    }

    private StockPrice stockPrice(Long stockId, String ticker, LocalDate baseDate) {
        Stock stock = mock(Stock.class);
        when(stock.getTicker()).thenReturn(ticker);

        StockPrice stockPrice = mock(StockPrice.class);
        StockPriceId stockPriceId = mock(StockPriceId.class);
        when(stockPriceId.getStockId()).thenReturn(stockId);
        when(stockPriceId.getBaseDate()).thenReturn(baseDate);
        when(stockPrice.getId()).thenReturn(stockPriceId);
        when(stockPrice.getStock()).thenReturn(stock);
        return stockPrice;
    }
}
