package org.stockwellness.domain.common.cache;

import java.time.Duration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    // 인증/회원
    MEMBER("member:v2", Duration.ofMinutes(30), true),

    // 주식 도메인
    AI_ANALYSIS("ai_analysis:v2", Duration.ofHours(24), false),

    // 섹터 도메인
    SECTOR_RANKING("sectorRanking:v3", Duration.ofHours(24), false),
    SECTOR_SUPPLY("sectorSupply:v3", Duration.ofHours(24), false),
    SECTOR_DETAIL("sectorDetail:v3", Duration.ofHours(24), false),
    SECTOR_COMPARISON("sectorComparison:v3", Duration.ofHours(24), false),
    MARKET_DASHBOARD("marketDashboard:v1", Duration.ofMinutes(5), false),

    // 시세 및 시장 지표 (성능 최적화용)
    STOCK_PRICE_YEAR("stockPriceYear:v1", Duration.ofHours(24), false),
    MARKET_BREADTH("marketBreadth:v1", Duration.ofHours(24), false),
    STOCK_SUPPLY_RANKING("stockSupplyRanking:v1", Duration.ofMinutes(10), false);

    private final String cacheName;
    private final Duration ttl;
    private final boolean useSecuritySerializer;
}
