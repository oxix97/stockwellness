package org.stockwellness.adapter.out.persistence.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.domain.stock.insight.SectorInsight;
import static org.stockwellness.domain.stock.insight.QSectorInsight.sectorInsight;

@RequiredArgsConstructor
public class SectorInsightRepositoryImpl implements SectorInsightRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<String, SectorInsight> findLatestBeforeByCodes(List<String> sectorCodes, LocalDate date) {
        if (sectorCodes == null || sectorCodes.isEmpty()) {
            return Map.of();
        }

        DateExpression<LocalDate> latestBaseDate = sectorInsight.baseDate.max();
        List<Tuple> latestDates = queryFactory
                .select(sectorInsight.sectorCode, latestBaseDate)
                .from(sectorInsight)
                .where(
                        sectorInsight.sectorCode.in(sectorCodes),
                        sectorInsight.baseDate.lt(date)
                )
                .groupBy(sectorInsight.sectorCode)
                .fetch();

        if (latestDates.isEmpty()) {
            return Map.of();
        }

        BooleanBuilder latestCondition = new BooleanBuilder();
        for (Tuple latestDate : latestDates) {
            String sectorCode = latestDate.get(sectorInsight.sectorCode);
            LocalDate baseDate = latestDate.get(latestBaseDate);
            if (sectorCode != null && baseDate != null) {
                latestCondition.or(
                        sectorInsight.sectorCode.eq(sectorCode)
                                .and(sectorInsight.baseDate.eq(baseDate))
                );
            }
        }

        if (!latestCondition.hasValue()) {
            return Map.of();
        }

        return queryFactory
                .selectFrom(sectorInsight)
                .where(latestCondition)
                .fetch()
                .stream()
                .collect(LinkedHashMap::new, (map, insight) -> map.put(insight.getSectorCode(), insight), Map::putAll);
    }

    @Override
    public Map<String, List<BigDecimal>> findPastPricesByCodes(List<String> sectorCodes, LocalDate date, int limit) {
        if (sectorCodes == null || sectorCodes.isEmpty() || limit <= 0) {
            return Map.of();
        }

        List<Tuple> rows = queryFactory
                .select(sectorInsight.sectorCode, sectorInsight.indicators.sectorIndexCurrentPrice)
                .from(sectorInsight)
                .where(
                        sectorInsight.sectorCode.in(sectorCodes),
                        sectorInsight.baseDate.lt(date)
                )
                .orderBy(sectorInsight.sectorCode.asc(), sectorInsight.baseDate.desc())
                .fetch();

        Map<String, List<BigDecimal>> result = new LinkedHashMap<>();
        for (Tuple row : rows) {
            String sectorCode = row.get(sectorInsight.sectorCode);
            BigDecimal price = row.get(sectorInsight.indicators.sectorIndexCurrentPrice);
            if (sectorCode == null || price == null) {
                continue;
            }
            List<BigDecimal> prices = result.computeIfAbsent(sectorCode, key -> new ArrayList<>());
            if (prices.size() < limit) {
                prices.add(price);
            }
        }

        return result;
    }
}
