package org.stockwellness.adapter.batch.stock.snapshot;

import java.io.Serializable;

import org.stockwellness.domain.stock.MarketType;

public record StockMasterSnapshot(
        MarketType marketType,
        String artifactPath,
        String sha256,
        long byteCount,
        int recordCount,
        String completenessStatus
) implements Serializable {

    public StockMasterSnapshot {
        if (marketType == null || artifactPath == null || artifactPath.isBlank()
                || sha256 == null || sha256.isBlank() || byteCount < 0 || recordCount < 0
                || completenessStatus == null || completenessStatus.isBlank()) {
            throw new IllegalArgumentException("Invalid stock master snapshot descriptor");
        }
    }
}
