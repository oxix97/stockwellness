package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;

@Entity
@Table(
        name = "index_constituent",
        indexes = {
                @Index(name = "idx_constituent_index", columnList = "index_code"),
                @Index(name = "idx_constituent_ticker", columnList = "stock_ticker")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndexConstituent extends AbstractEntity {

    // 객체 참조(@ManyToOne) 대신 논리적 외래키(String) 사용 (MSA 전환 및 Batch 성능 고려)
    @Column(name = "index_code", nullable = false, length = 4)
    private String indexCode;

    @Column(name = "stock_ticker", nullable = false, length = 20)
    private String stockTicker;

    // 💡 퀀트 팁: 향후 포트폴리오 시뮬레이션을 위해 '편입 비중(Weight)' 컬럼을 추가해두면 매우 유용합니다.
    @Column(name = "weight_percent", precision = 5, scale = 2)
    private BigDecimal weightPercent;

    public static IndexConstituent of(String indexCode, String stockTicker, BigDecimal weightPercent) {
        var entity = new IndexConstituent();
        entity.indexCode = indexCode;
        entity.stockTicker = stockTicker;
        entity.weightPercent = weightPercent;
        return entity;
    }
}