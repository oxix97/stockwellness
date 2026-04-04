package org.stockwellness.adapter.out.persistence.stock.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorIndicators;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@DisplayName("SectorInsightRepository 통합 테스트")
class SectorInsightRepositoryTest {

    @Autowired
    private SectorInsightRepository sectorInsightRepository;

    @Test
    @DisplayName("업종 코드와 기준일로 업종 인사이트를 조회한다")
    void findBySectorCodeAndBaseDate_Success() {
        // given
        LocalDate today = LocalDate.now();
        SectorIndicators indicators = SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1.5), 100L, 50L, 3, 2);
        SectorInsight insight = SectorInsight.of("전기전자", "001", MarketType.KOSPI, today, indicators, null, false);
        sectorInsightRepository.save(insight);
        sectorInsightRepository.flush();

        // when
        Optional<SectorInsight> found = sectorInsightRepository.findBySectorCodeAndBaseDate("001", today);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSectorName()).isEqualTo("전기전자");
    }

    @Test
    @DisplayName("특정 날짜의 등락률 상위 업종을 조회한다")
    void findTopByDate_Success() {
        // given
        LocalDate today = LocalDate.now();
        SectorInsight s1 = SectorInsight.of("IT", "001", MarketType.KOSPI, today, 
                SectorIndicators.of(BigDecimal.valueOf(100), BigDecimal.valueOf(5.0), 0L, 0L, 0, 0), null, false);
        SectorInsight s2 = SectorInsight.of("Finance", "002", MarketType.KOSPI, today, 
                SectorIndicators.of(BigDecimal.valueOf(100), BigDecimal.valueOf(2.0), 0L, 0L, 0, 0), null, false);
        SectorInsight s3 = SectorInsight.of("Bio", "003", MarketType.KOSPI, today, 
                SectorIndicators.of(BigDecimal.valueOf(100), BigDecimal.valueOf(10.0), 0L, 0L, 0, 0), null, false);
        
        sectorInsightRepository.saveAll(List.of(s1, s2, s3));
        sectorInsightRepository.flush();

        // when: 등락률 내림차순 상위 2개
        List<SectorInsight> topSectors = sectorInsightRepository.findTopByDate(today, MarketType.KOSPI, PageRequest.of(0, 2));

        // then
        assertThat(topSectors).hasSize(2);
        assertThat(topSectors.get(0).getSectorName()).isEqualTo("Bio");
        assertThat(topSectors.get(1).getSectorName()).isEqualTo("IT");
    }

    @Test
    @DisplayName("특정 날짜의 수급(외국인/기관 매수) 상위 업종을 조회한다")
    void findTopBySupply_Success() {
        // given
        LocalDate today = LocalDate.now();
        SectorInsight s1 = SectorInsight.of("IT", "001", MarketType.KOSPI, today, 
                SectorIndicators.of(BigDecimal.valueOf(100), BigDecimal.ZERO, 500L, 100L, 5, 1), null, false);
        SectorInsight s2 = SectorInsight.of("Finance", "002", MarketType.KOSPI, today, 
                SectorIndicators.of(BigDecimal.valueOf(100), BigDecimal.ZERO, 1000L, 200L, 10, 2), null, false);
        
        sectorInsightRepository.saveAll(List.of(s1, s2));
        sectorInsightRepository.flush();

        // when
        List<SectorInsight> topSupply = sectorInsightRepository.findTopBySupply(today, MarketType.KOSPI, PageRequest.of(0, 10));

        // then
        assertThat(topSupply.get(0).getSectorName()).isEqualTo("Finance");
    }

    @Test
    @DisplayName("가장 최신 기준일을 조회한다")
    void findMaxBaseDate_Success() {
        // given
        LocalDate d1 = LocalDate.of(2026, 1, 1);
        LocalDate d2 = LocalDate.of(2026, 2, 1);
        
        SectorInsight s1 = SectorInsight.of("T1", "001", MarketType.KOSPI, d1, 
                SectorIndicators.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0, 0), null, false);
        SectorInsight s2 = SectorInsight.of("T2", "002", MarketType.KOSPI, d2, 
                SectorIndicators.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0, 0), null, false);
        
        sectorInsightRepository.saveAll(List.of(s1, s2));
        sectorInsightRepository.flush();

        // when
        Optional<LocalDate> maxDate = sectorInsightRepository.findMaxBaseDate();

        // then
        assertThat(maxDate).isPresent();
        assertThat(maxDate.get()).isEqualTo(d2);
    }
}
