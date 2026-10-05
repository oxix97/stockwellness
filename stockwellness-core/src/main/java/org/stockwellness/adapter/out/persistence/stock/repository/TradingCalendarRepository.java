package org.stockwellness.adapter.out.persistence.stock.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.TradingCalendarId;

public interface TradingCalendarRepository extends JpaRepository<TradingCalendar, TradingCalendarId> {
    Optional<TradingCalendar> findByIdMarketCodeAndIdTradeDate(String marketCode, LocalDate tradeDate);

    List<TradingCalendar> findByIdMarketCodeAndIdTradeDateBetweenOrderByIdTradeDateAsc(
            String marketCode, LocalDate startDate, LocalDate endDate);
    Optional<TradingCalendar> findFirstByIdMarketCodeAndTradingDayTrueAndIdTradeDateLessThanEqualOrderByIdTradeDateDesc(
            String marketCode, LocalDate tradeDate);
}
