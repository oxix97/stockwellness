package org.stockwellness.batch.job.stock.master;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBatchTest
@SpringBootTest
class StockMasterSyncJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("stockMasterSyncJob")
    public void setJob(Job job) {
        this.jobLauncherTestUtils.setJob(job);
    }

    @MockitoBean
    private KisMasterClient kisMasterClient;

    @Autowired
    private MarketIndexRepository marketIndexRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @BeforeEach
    void setUp() {
        stockPriceRepository.deleteAllInBatch();
        stockRepository.deleteAllInBatch();
        marketIndexRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("전체 종목 마스터 동기화 Job이 실행되면 Upsert와 상장폐지가 일관되게 처리된다")
    void stockMasterSyncJobTest() throws Exception {
        // given
        // 1. 초기 데이터 설정 (0012 -> 전기전자)
        marketIndexRepository.save(new MarketIndex("0012", "전기전자"));
        
        // 2. DB 상황: 삼성전자는 이미 있고, LG전자는 새로 상장되는 상황 재현
        Stock existingSamsung = Stock.ofKospi(
                createMockItem("005930", "삼성전자", "0012"),
                StockSector.of("0001", "0012", "0000", "전기전자")
        );
        stockRepository.save(existingSamsung);

        // 3. KIS 마스터 Mock: 삼성전자가 파일에서 사라짐 (상장폐지 대상), LG전자만 새로 나타남 (신규등록 대상)
        String lgeLine = createMockKospiLine("066570", "KR7066570003", "LG전자", "0012");
        given(kisMasterClient.downloadKospiMaster()).willReturn(List.of(lgeLine));
        given(kisMasterClient.downloadKosdaqMaster()).willReturn(List.of()); // 코스닥은 빈 상태

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        
        // 삼성전자 검증: 마스터 파일에 없으므로 DELISTED 처리 확인
        Stock samsung = stockRepository.findByTicker("005930").orElseThrow();
        assertThat(samsung.getStatus()).isEqualTo(StockStatus.DELISTED);
        
        // LG전자 검증: 신규 등록 및 업종 매핑 확인
        Stock lge = stockRepository.findByTicker("066570").orElseThrow();
        assertThat(lge.getStatus()).isEqualTo(StockStatus.ACTIVE);
        assertThat(lge.getSector().getSectorName()).isEqualTo("전기전자");
    }

    private KospiItem createMockItem(String shortCode, String name, String mediumSector) {
        return KospiMstParser.parseLines(List.of(createMockKospiLine(shortCode, "ISIN", name, mediumSector))).get(0);
    }

    private String createMockKospiLine(String shortCode, String isin, String name, String mediumSector) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-9s", shortCode));
        sb.append(String.format("%-12s", isin));
        sb.append(String.format("%-40s", name));
        
        sb.append("ST");
        sb.append("1");
        sb.append("0001");
        sb.append(String.format("%-4s", mediumSector));
        
        sb.append(" ".repeat(216));
        return sb.toString();
    }
}
