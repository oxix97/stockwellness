package org.stockwellness.adapter.out.persistence.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.application.port.out.stock.SectorDailyDetailSnapshot;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@DisplayName("SectorDailyDetailRepository 통합 테스트")
class SectorDailyDetailRepositoryTest {

    @Autowired
    private SectorDailyDetailRepository sectorDailyDetailRepository;

    @Test
    @DisplayName("섹터 코드와 기준일로 원천 상세를 조회한다")
    void findBySectorCodeAndBaseDate_success() {
        LocalDate today = LocalDate.of(2026, 4, 9);
        SectorDailyDetail detail = newDetail("0029", "전기전자", today, 100L, 50L);
        sectorDailyDetailRepository.saveAndFlush(detail);

        Optional<SectorDailyDetail> found = sectorDailyDetailRepository.findBySectorCodeAndBaseDate("0029", today);

        assertThat(found).isPresent();
        assertThat(found.get().getSectorName()).isEqualTo("전기전자");
        assertThat(found.get().getCurrentPrice()).isEqualByComparingTo("1000.12");
    }

    @Test
    @DisplayName("기준일 조회 시 섹터 코드 오름차순으로 반환한다")
    void findByBaseDateOrderBySectorCodeAsc_success() {
        LocalDate today = LocalDate.of(2026, 4, 9);
        sectorDailyDetailRepository.saveAllAndFlush(List.of(
                newDetail("1010", "반도체", today, 10L, 20L),
                newDetail("0029", "전기전자", today, 30L, 40L)
        ));

        List<SectorDailyDetail> found = sectorDailyDetailRepository.findByBaseDateOrderBySectorCodeAsc(today);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getSectorCode()).isEqualTo("0029");
        assertThat(found.get(1).getSectorCode()).isEqualTo("1010");
    }

    @Test
    @DisplayName("섹터 코드 목록과 기준일로 기존 상세를 한 번에 조회한다")
    void findBySectorCodeInAndBaseDate_success() {
        LocalDate today = LocalDate.of(2026, 4, 9);
        sectorDailyDetailRepository.saveAllAndFlush(List.of(
                newDetail("0029", "전기전자", today, 30L, 40L),
                newDetail("1010", "반도체", today, 10L, 20L),
                newDetail("2020", "자동차", today.minusDays(1), 50L, 60L)
        ));

        List<SectorDailyDetail> found = sectorDailyDetailRepository.findBySectorCodeInAndBaseDate(List.of("0029", "2020"), today);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getSectorCode()).isEqualTo("0029");
    }

    private SectorDailyDetail newDetail(String sectorCode, String sectorName, LocalDate baseDate, long foreign, long inst) {
        return SectorDailyDetail.of(
                sectorCode,
                sectorName,
                new SectorDailyDetailSnapshot(
                        sectorCode,
                        baseDate,
                        new BigDecimal("1000.12"),
                        new BigDecimal("12.34"),
                        "2",
                        new BigDecimal("1.23"),
                        100L,
                        90L,
                        1000L,
                        900L,
                        new BigDecimal("990.00"),
                        new BigDecimal("1010.00"),
                        new BigDecimal("980.00"),
                        10,
                        1,
                        2,
                        3,
                        0,
                        new BigDecimal("1100.00"),
                        new BigDecimal("-9.08"),
                        baseDate,
                        new BigDecimal("800.00"),
                        new BigDecimal("25.01"),
                        baseDate.minusMonths(1),
                        100L,
                        200L,
                        new BigDecimal("33.33"),
                        new BigDecimal("66.67"),
                        100L,
                        foreign,
                        inst
                )
        );
    }
}
