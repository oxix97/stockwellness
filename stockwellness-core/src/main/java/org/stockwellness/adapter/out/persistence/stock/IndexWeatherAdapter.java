package org.stockwellness.adapter.out.persistence.stock;

import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.IndexWeatherPort;
import org.stockwellness.domain.stock.insight.IndexWeather;

@Component
@RequiredArgsConstructor
public class IndexWeatherAdapter implements IndexWeatherPort {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void upsert(IndexWeather weather) {
        jdbc.update("""
                INSERT INTO index_weather AS existing
                    (series_id, trade_date, score, status, score_version, input_manifest, calculated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (series_id, trade_date) DO UPDATE SET
                    score = EXCLUDED.score, status = EXCLUDED.status, score_version = EXCLUDED.score_version,
                    input_manifest = EXCLUDED.input_manifest, calculated_at = EXCLUDED.calculated_at
                WHERE (existing.score, existing.status, existing.score_version, existing.input_manifest)
                    IS DISTINCT FROM
                      (EXCLUDED.score, EXCLUDED.status, EXCLUDED.score_version, EXCLUDED.input_manifest)
                """, weather.seriesId(), weather.tradeDate(), weather.score(), weather.status(),
                weather.scoreVersion(), weather.inputManifest(), Timestamp.from(weather.calculatedAt()));
    }
}
