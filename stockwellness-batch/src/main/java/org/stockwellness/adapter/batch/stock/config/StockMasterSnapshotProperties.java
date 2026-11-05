package org.stockwellness.adapter.batch.stock.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stockwellness.batch.stock-master")
public record StockMasterSnapshotProperties(
        Path stagingDirectory,
        Duration retention
) {

    public StockMasterSnapshotProperties {
        if (stagingDirectory == null) {
            throw new IllegalArgumentException("Stock master staging directory is required");
        }
        stagingDirectory = stagingDirectory.toAbsolutePath().normalize();
        retention = retention == null ? Duration.ofDays(7) : retention;
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("Stock master snapshot retention must be positive");
        }
    }
}
