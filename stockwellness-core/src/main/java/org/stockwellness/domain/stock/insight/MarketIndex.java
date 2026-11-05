package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.shared.AbstractEntity;
import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "market_index", indexes = @Index(name = "idx_market_index_market", columnList = "market_code"),
        uniqueConstraints = @UniqueConstraint(name = "uq_market_index_provider_identity",
                columnNames = {"provider", "market_code", "provider_code"}))
public class MarketIndex extends AbstractEntity {

    @Column(name = "index_code", nullable = false, length = 10, unique = true)
    private String indexCode;

    @Column(name = "index_name", nullable = false, length = 100)
    private String indexName;

    @Column(nullable = false, length = 30)
    private String provider = "KIS";

    @Column(name = "provider_code", nullable = false, length = 20)
    private String providerCode;

    @Column(name = "market_code", length = 20)
    private String marketCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "index_kind", nullable = false, length = 20)
    private MarketIndexKind indexKind = MarketIndexKind.OTHER;

    @Column(name = "source_updated_at")
    private java.time.OffsetDateTime sourceUpdatedAt;

    public MarketIndex(String indexCode, String indexName) {
        this.indexCode = indexCode;
        this.indexName = indexName;
        this.providerCode = indexCode;
    }

    public static MarketIndex of(String rawCode, String indexName) {
        if (rawCode == null || rawCode.isEmpty()) {
            throw new IllegalArgumentException("MarketIndex 생성 오류: 원천 코드는 비어있을 수 없습니다. 입력값=" + rawCode);
        }
        var entity = new MarketIndex();
        entity.indexCode = rawCode; // Store the full rawCode
        entity.indexName = indexName;
        entity.providerCode = rawCode;
        return entity;
    }

    public static MarketIndex of(String rawCode, String indexName, String division) {
        MarketIndex entity = of(rawCode, indexName);
        if ("0".equals(division)) entity.marketCode = "KOSPI";
        else if ("1".equals(division)) entity.marketCode = "KOSDAQ";
        if (("0".equals(division) && "0001".equals(rawCode))
                || ("1".equals(division) && "1001".equals(rawCode))) {
            entity.indexKind = MarketIndexKind.MARKET;
        }
        return entity;
    }

    public boolean updateFrom(MarketIndex source) {
        boolean changed = !java.util.Objects.equals(indexName, source.indexName)
                || !java.util.Objects.equals(providerCode, source.providerCode)
                || !java.util.Objects.equals(marketCode, source.marketCode)
                || indexKind != source.indexKind;
        if (changed) {
            indexName = source.indexName;
            provider = source.provider;
            providerCode = source.providerCode;
            marketCode = source.marketCode;
            indexKind = source.indexKind;
            sourceUpdatedAt = source.sourceUpdatedAt;
        }
        return changed;
    }
}
