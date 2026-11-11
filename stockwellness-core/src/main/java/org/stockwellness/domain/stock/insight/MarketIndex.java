package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.stock.BenchmarkType;
import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "market_index")
public class MarketIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "index_key", nullable = false, length = 80, unique = true)
    private String indexKey;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private MarketIndexKind kind = MarketIndexKind.OTHER;

    @Column(name = "public_benchmark_code", length = 30, unique = true)
    private String publicBenchmarkCode;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    // Provider identity belongs to IndexSeries; these fields only carry parser input until it is persisted.
    @Transient
    private String provider = "KIS";
    @Transient
    private String providerCode;
    @Transient
    private String marketCode;
    @Transient
    private java.time.OffsetDateTime sourceUpdatedAt;

    public MarketIndex(String indexCode, String indexName) {
        this.indexKey = indexCode;
        this.name = indexName;
        this.providerCode = indexCode;
    }

    public static MarketIndex of(String rawCode, String indexName) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("MarketIndex 생성 오류: 원천 코드는 비어있을 수 없습니다. 입력값=" + rawCode);
        }
        return new MarketIndex(rawCode, indexName);
    }

    public static MarketIndex of(String rawCode, String indexName, String division) {
        MarketIndex entity = of(rawCode, indexName);
        entity.marketCode = switch (division) {
            case "0" -> "KOSPI";
            case "1" -> "KOSDAQ";
            default -> null;
        };
        entity.indexKey = entity.marketCode == null ? "UNKNOWN:" + division + ":" + rawCode
                : entity.marketCode + ":" + rawCode;
        if (("0".equals(division) && "0001".equals(rawCode))
                || ("1".equals(division) && "1001".equals(rawCode))) {
            entity.kind = MarketIndexKind.MARKET;
        } else if (entity.marketCode != null) {
            entity.kind = MarketIndexKind.SECTOR;
        }
        entity.publicBenchmarkCode = supportedBenchmarkCode(rawCode);
        return entity;
    }

    public String getIndexCode() {
        return providerCode == null ? indexKey : providerCode;
    }

    public String getIndexName() {
        return name;
    }

    public MarketIndexKind getIndexKind() {
        return kind;
    }

    public boolean updateFrom(MarketIndex source) {
        boolean changed = !java.util.Objects.equals(name, source.name) || kind != source.kind
                || !java.util.Objects.equals(publicBenchmarkCode, source.publicBenchmarkCode);
        if (changed) {
            name = source.name;
            kind = source.kind;
            publicBenchmarkCode = source.publicBenchmarkCode;
            provider = source.provider;
            providerCode = source.providerCode;
            marketCode = source.marketCode;
            sourceUpdatedAt = source.sourceUpdatedAt;
        }
        return changed;
    }

    private static String supportedBenchmarkCode(String code) {
        for (BenchmarkType benchmark : BenchmarkType.values()) {
            if (benchmark.getTicker().equalsIgnoreCase(code)) return benchmark.getTicker();
        }
        return null;
    }
}
