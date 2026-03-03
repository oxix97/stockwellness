package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.shared.AbstractEntity;

import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString
@NoArgsConstructor(access = PROTECTED)
@Entity
public class MarketIndex extends AbstractEntity {

    @Column(name = "index_code", nullable = false, length = 10, unique = true)
    private String indexCode;

    @Column(name = "index_name", nullable = false, length = 100)
    private String indexName;

    public MarketIndex(String indexCode, String indexName) {
        this.indexCode = indexCode;
        this.indexName = indexName;
    }

    public static MarketIndex of(String rawCode, String indexName) {
        if (rawCode == null || rawCode.isEmpty()) {
            throw new IllegalArgumentException("MarketIndex 생성 오류: 원천 코드는 비어있을 수 없습니다. 입력값=" + rawCode);
        }
        var entity = new MarketIndex();
        entity.indexCode = rawCode; // Store the full rawCode
        entity.indexName = indexName;
        return entity;
    }
}
