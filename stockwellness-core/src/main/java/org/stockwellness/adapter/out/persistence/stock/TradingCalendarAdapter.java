package org.stockwellness.adapter.out.persistence.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.TradingCalendarRepository;
import org.stockwellness.application.port.out.stock.TradingCalendarPort;
import org.stockwellness.domain.stock.TradingCalendar;

@Component
@RequiredArgsConstructor
public class TradingCalendarAdapter implements TradingCalendarPort {
    private final TradingCalendarRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingCalendar> find(String marketCode, LocalDate tradeDate) {
        return repository.findByIdMarketCodeAndIdTradeDate(marketCode, tradeDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradingCalendar> findAll(String marketCode, LocalDate startDate, LocalDate endDate) {
        return repository.findByIdMarketCodeAndIdTradeDateBetweenOrderByIdTradeDateAsc(marketCode, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> latestTradingDayOnOrBefore(String marketCode, LocalDate tradeDate) {
        return repository.findFirstByIdMarketCodeAndTradingDayTrueAndIdTradeDateLessThanEqualOrderByIdTradeDateDesc(
                        marketCode, tradeDate)
                .map(TradingCalendar::getTradeDate);
    }

    @Override
    @Transactional
    public void upsert(List<TradingCalendar> rows) {
        if (rows == null || rows.isEmpty()) return;
        repository.saveAll(rows);
        repository.flush();
    }
}
