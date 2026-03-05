package org.stockwellness.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.Map;

import static lombok.AccessLevel.PROTECTED;

/**
 * 종목의 업종(Sector) 정보 통합 관리 임베디드 타입
 *
 * <p>KIS 마스터의 대/중/소분류 코드를 저장하며,
 * {@code sectorName}은 우선순위(소 > 중 > 대)에 따라 {@link MarketIndex}와 매핑하여 결정됩니다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = PROTECTED)
public class StockSector {

    @Column(name = "sector_large_code", length = 10)
    private String largeCode;

    @Column(name = "sector_medium_code", length = 10)
    private String mediumCode;

    @Column(name = "sector_small_code", length = 10)
    private String smallCode;

    /**
     * 최종 매핑된 업종명 (e.g. "제약", "반도체").
     * 매핑된 이름이 없을 경우 기본값이 저장됩니다.
     */
    @Column(name = "sector_name", length = 100)
    private String sectorName;

    private StockSector(String largeCode, String mediumCode, String smallCode, String sectorName) {
        this.largeCode = largeCode;
        this.mediumCode = mediumCode;
        this.smallCode = smallCode;
        this.sectorName = sectorName;
    }

    /**
     * KIS 마스터 코드를 바탕으로 {@link StockSector}를 생성합니다.
     *
     * @param large    대분류 코드
     * @param medium   중분류 코드
     * @param small    소분류 코드
     * @param indexMap 활성 업종 인덱스 맵 (Key: indexCode, Value: MarketIndex)
     * @return 생성된 StockSector 객체
     */
    public static StockSector of(String large, String medium, String small, Map<String, MarketIndex> indexMap) {
        // 모든 입력 코드에 대해 trim() 적용하여 정규화
        String l = (large != null) ? large.trim() : null;
        String m = (medium != null) ? medium.trim() : null;
        String s = (small != null) ? small.trim() : null;

        // 우선순위: 소(Small) > 중(Medium) > 대(Large)
        String resolvedName = resolveName(s, indexMap);
        if (resolvedName == null) {
            resolvedName = resolveName(m, indexMap);
        }
        if (resolvedName == null) {
            resolvedName = resolveName(l, indexMap);
        }

        // 기본값 처리: 매핑된 이름이 없으면 대분류 코드 자체 사용, 그것도 없으면 "미분류"
        if (resolvedName == null) {
            resolvedName = (l != null && !l.isBlank()) ? l : "미분류";
        }

        return new StockSector(l, m, s, resolvedName);
    }

    /**
     * 단순 코드 정보만으로 생성 (해외 종목 등)
     */
    public static StockSector of(String large, String medium, String small, String name) {
        return new StockSector(
                large != null ? large.trim() : null,
                medium != null ? medium.trim() : null,
                small != null ? small.trim() : null,
                name
        );
    }

    /**
     * 기본값(모두 null)으로 생성
     */
    public static StockSector empty() {
        return new StockSector(null, null, null, "미분류");
    }

    private static String resolveName(String code, Map<String, MarketIndex> indexMap) {
        if (code == null || code.isBlank() || "0000".equals(code.trim())) {
            return null;
        }
        // Map 조회 시에도 trim된 키 사용 보장
        MarketIndex index = indexMap.get(code.trim());
        return (index != null) ? index.getIndexName() : null;
    }
}
