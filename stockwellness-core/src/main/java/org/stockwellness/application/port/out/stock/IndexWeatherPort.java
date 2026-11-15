package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.insight.IndexWeather;

public interface IndexWeatherPort {
    void upsert(IndexWeather weather);
}
