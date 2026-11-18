package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvalidPriceObservation(String ticker, String name, LocalDate tradeDate, BigDecimal closePrice) {}
