package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.stockwellness.adapter.out.persistence.stock.repository.TradingCalendarRepository;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.TradingCalendar;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@EntityScan(basePackageClasses = TradingCalendar.class)
@EnableJpaRepositories(basePackageClasses = TradingCalendarRepository.class)
@Import({TradingCalendarAdapter.class, QueryDslConfig.class})
class TradingCalendarPostgresTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withImagePullPolicy(image -> false);

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired TradingCalendarAdapter calendars;
    @Autowired TradingCalendarRepository repository;

    @Test
    void closed_and_unknown_days_are_distinct_and_latest_open_date_skips_closed_rows() {
        LocalDate open = LocalDate.of(2026, 10, 2);
        LocalDate closed = LocalDate.of(2026, 10, 3);
        calendars.upsert(List.of(
                TradingCalendar.of("KOSPI", open, true, at(open, 9), at(open, 15), "test"),
                TradingCalendar.of("KOSPI", closed, false, null, null, "test")));

        assertThat(calendars.find("KOSPI", closed)).get().extracting(TradingCalendar::isTradingDay).isEqualTo(false);
        assertThat(calendars.find("KOSPI", LocalDate.of(2026, 10, 4))).isEmpty();
        assertThat(calendars.latestTradingDayOnOrBefore("KOSPI", closed)).contains(open);
        assertThat(calendars.latestTradingDayOnOrBefore("NASDAQ", closed)).isEmpty();
    }

    @Test
    void repeated_upsert_is_idempotent_and_authoritative_correction_updates_existing_row() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        TradingCalendar closed = TradingCalendar.of("KOSPI", date, false, null, null, "test");
        TradingCalendar corrected = TradingCalendar.of("KOSPI", date, true,
                at(date, 9), at(date, 15), "test-correction");

        calendars.upsert(List.of(closed));
        calendars.upsert(List.of(closed));
        assertThat(repository.count()).isEqualTo(1);
        calendars.upsert(List.of(corrected));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(calendars.find("KOSPI", date)).get().satisfies(row -> {
            assertThat(row.isTradingDay()).isTrue();
            assertThat(row.getSource()).isEqualTo("test-correction");
            assertThat(row.getOpensAt()).isEqualTo(at(date, 9));
        });
    }

    private static OffsetDateTime at(LocalDate date, int hour) {
        return date.atTime(hour, 0).atOffset(ZoneOffset.ofHours(9));
    }
}
