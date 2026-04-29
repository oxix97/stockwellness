package org.stockwellness.application.port.out.messaging;

import java.time.LocalDate;
import java.util.List;

public record MarketScoreCalculatedEvent(
    LocalDate baseDate,
    String marketType,
    int overallScore,
    List<SectorScore> sectorScores
) {
    public record SectorScore(String code, String name, int score) {}
}
