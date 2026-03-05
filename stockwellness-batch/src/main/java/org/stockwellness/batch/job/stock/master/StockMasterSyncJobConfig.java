package org.stockwellness.batch.job.stock.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

import java.util.List;

/**
 * 종목 마스터 동기화 Spring Batch Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockMasterSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final KisMasterClient kisMasterClient;
    private final StockRepository stockRepository;
    private final MarketIndexRepository marketIndexRepository;

    private static final int CHUNK_SIZE = 500;

    @Bean
    public Job stockMasterSyncJob() {
        return new JobBuilder("stockMasterSyncJob", jobRepository)
                .listener(new StockMasterJobExecutionListener())
                .start(kospiUpsertStep())
                .next(kospiDelistStep())
                .next(kosdaqUpsertStep())
                .next(kosdaqDelistStep())
                .build();
    }

    // ── KOSPI Steps ───────────────────────────────────────────────────────────

    @Bean
    public Step kospiUpsertStep() {
        return new StepBuilder("kospiUpsertStep", jobRepository)
                .<KospiItem, Stock>chunk(CHUNK_SIZE, txManager)
                .reader(kospiItemReader())
                .processor(kospiItemProcessor())
                .writer(stockItemWriter())
                .listener(new StockMasterStepExecutionListener("KOSPI-Upsert"))
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }

    @Bean
    public Step kospiDelistStep() {
        return new StepBuilder("kospiDelistStep", jobRepository)
                .tasklet(new StockDelistTasklet(stockRepository, MarketType.KOSPI), txManager)
                .listener(new StockMasterStepExecutionListener("KOSPI-Delist"))
                .build();
    }

    // ── KOSDAQ Steps ──────────────────────────────────────────────────────────

    @Bean
    public Step kosdaqUpsertStep() {
        return new StepBuilder("kosdaqUpsertStep", jobRepository)
                .<KosdaqItem, Stock>chunk(CHUNK_SIZE, txManager)
                .reader(kosdaqItemReader())
                .processor(kosdaqItemProcessor())
                .writer(stockItemWriter())
                .listener(new StockMasterStepExecutionListener("KOSDAQ-Upsert"))
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .build();
    }

    @Bean
    public Step kosdaqDelistStep() {
        return new StepBuilder("kosdaqDelistStep", jobRepository)
                .tasklet(new StockDelistTasklet(stockRepository, MarketType.KOSDAQ), txManager)
                .listener(new StockMasterStepExecutionListener("KOSDAQ-Delist"))
                .build();
    }

    // ── Readers ───────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public KospiMasterItemReader kospiItemReader() {
        log.info("[KOSPI] 마스터 파일 다운로드 시작");
        List<String> rawLines = kisMasterClient.downloadKospiMaster();
        List<KospiItem> items = KospiMstParser.parseLines(rawLines);
        log.info("[KOSPI] 파싱 완료: {}건", items.size());
        return new KospiMasterItemReader(items);
    }

    @Bean
    @StepScope
    public KosdaqMasterItemReader kosdaqItemReader() {
        log.info("[KOSDAQ] 마스터 파일 다운로드 시작");
        List<String> rawLines = kisMasterClient.downloadKosdaqMaster();
        List<KosdaqItem> items = KosdaqMstParser.parseLines(rawLines);
        log.info("[KOSDAQ] 파싱 완료: {}건", items.size());
        return new KosdaqMasterItemReader(items);
    }

    // ── Processors ────────────────────────────────────────────────────────────

    @Bean
    @StepScope
    public StockItemProcessor.Kospi kospiItemProcessor() {
        return new StockItemProcessor.Kospi(
                stockRepository,
                marketIndexRepository.findActiveIndexMap()
        );
    }

    @Bean
    @StepScope
    public StockItemProcessor.Kosdaq kosdaqItemProcessor() {
        return new StockItemProcessor.Kosdaq(
                stockRepository,
                marketIndexRepository.findActiveIndexMap()
        );
    }

    // ── Writer ────────────────────────────────────────────────────────────────

    @Bean
    public ItemWriter<Stock> stockItemWriter() {
        return chunk -> stockRepository.saveAll(chunk.getItems());
    }
}
